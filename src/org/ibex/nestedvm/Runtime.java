// Copyright 2000-2005 the Contributors, as shown in the revision logs.
// Licensed under the Apache Public Source License 2.0 ("the License").
// You may not use this file except in compliance with the License.

// Copyright 2003 Brian Alliet
// Based on org.xwt.imp.MIPS by Adam Megacz
// Portions Copyright 2003 Adam Megacz

package org.ibex.nestedvm;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.text.DateFormatSymbols;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

public abstract class Runtime implements Cloneable
{
    public static final String VERSION = "1.0";
    
    /** True to write useful diagnostic information to stderr when things go wrong */
    final static boolean STDERR_DIAG = true;
    
    /** Number of bits to shift to get the page number (1<<<pageShift == pageSize) */
    protected final int pageShift;
    /** Bottom of region of memory allocated to the stack */
    private final int stackBottom;
    
    /** Readable main memory pages */
    protected int[][] readPages;
    /** Writable main memory pages.
        If the page is writable writePages[x] == readPages[x]; if not writePages[x] == null. */
    protected int[][] writePages;
    
    /** The address of the end of the heap */
    private int heapEnd;
    
    /** Number of guard pages to keep between the stack and the heap */
    private static final int STACK_GUARD_PAGES = 4;
    
    /** The last address the executable uses (other than the heap/stack) */
    protected abstract int heapStart();
        
    /** The program's entry point */
    protected abstract int entryPoint();

    /** The location of the _user_info block (or 0 is there is none) */
    protected int userInfoBase() { return 0; }
    protected int userInfoSize() { return 0; }
    
    /** The location of the global pointer */
    protected abstract int gp();
    
    /** When the process started */
    private long startTime;
    
    /** Program is executing instructions */
    public final static int RUNNING = 0; // Horrible things will happen if this isn't 0
    /**  Text/Data loaded in memory  */
    public final static int STOPPED = 1;
    /** Prgram has been started but is paused */
    public final static int PAUSED = 2;
    /** Program is executing a callJava() method */
    public final static int CALLJAVA = 3;
    /** Program has exited (it cannot currently be restarted) */
    public final static int EXITED = 4;
    /** Program has executed a successful exec(), a new Runtime needs to be run (used by UnixRuntime) */
    public final static int EXECED = 5;
    
    /** The current state */
    protected int state = STOPPED;
    /** @see Runtime#state state */
    public final int getState() { return state; }
    
    /** The exit status if the process (only valid if state==DONE) 
        @see Runtime#state */
    private int exitStatus;
    public ExecutionException exitException;
    
    /** Table containing all open file descriptors. (Entries are null if the fd is not in use */
    FD[] fds; // package-private for UnixRuntime
    boolean closeOnExec[];
    
    /** Pointer to a SecurityManager for this process */
    SecurityManager sm;
    public void setSecurityManager(SecurityManager sm) { this.sm = sm; }
    
    /** Pointer to a callback for the call_java syscall */
    private CallJavaCB callJavaCB;
    public void setCallJavaCB(CallJavaCB callJavaCB) { this.callJavaCB = callJavaCB; }
        
    /** Temporary buffer for read/write operations */
    private byte[] _byteBuf;
    /** Max size of temporary buffer
        @see Runtime#_byteBuf */
    final static int MAX_CHUNK = 16*1024*1024 - 1024;
        
    /** Subclasses should actually execute program in this method. They should continue 
        executing until state != RUNNING. Only syscall() can modify state. It is safe 
        to only check the state attribute after a call to syscall() */
    protected abstract void _execute() throws ExecutionException;
    
    /** Subclasses should return the address of the symbol <i>symbol</i> or -1 it it doesn't exits in this method 
        This method is only required if the call() function is used */
    public int lookupSymbol(String symbol) { return -1; }
    
    /** Subclasses should populate a CPUState object representing the cpu state */
    protected abstract void getCPUState(CPUState state);
    
    /** Subclasses should set the CPUState to the state held in <i>state</i> */
    protected abstract void setCPUState(CPUState state);
    
    /** True to enabled a few hacks to better support the win32 console */
    final static boolean win32Hacks;
    
    static {
        String os = Platform.getProperty("os.name");
        String prop = Platform.getProperty("nestedvm.win32hacks");
        if(prop != null) { win32Hacks = Boolean.valueOf(prop).booleanValue(); }
        else { win32Hacks = os != null && os.toLowerCase().indexOf("windows") != -1; }
    }
    
    protected Object clone() throws CloneNotSupportedException {
        Runtime r = (Runtime) super.clone();
        r._byteBuf = null;
        r.startTime = 0;
        r.fds = new FD[OPEN_MAX];
        for(int i=0;i<OPEN_MAX;i++) if(fds[i] != null) r.fds[i] = fds[i].dup();
        int totalPages = writePages.length;
        r.readPages = new int[totalPages][];
        r.writePages = new int[totalPages][];
        for(int i=0;i<totalPages;i++) {
            if(readPages[i] == null) continue;
            if(writePages[i] == null) r.readPages[i] = readPages[i];
            else r.readPages[i] = r.writePages[i] = (int[])writePages[i].clone();
        }
        return r;
    }
    
    protected Runtime (PrintStream pw, int pageSize, int totalPages)
    {
        this (pw, pageSize, totalPages,false);
    }

    protected Runtime (PrintStream pw, int pageSize, int totalPages, boolean exec)
    {
        if(pageSize <= 0) throw new IllegalArgumentException("pageSize <= 0");
        if(totalPages <= 0) throw new IllegalArgumentException("totalPages <= 0");
        if((pageSize&(pageSize-1)) != 0) throw new IllegalArgumentException("pageSize not a power of two");

        int _pageShift = 0;
        while(pageSize>>>_pageShift != 1) _pageShift++;
        pageShift = _pageShift;
        
        int heapStart = heapStart();
        int totalMemory = totalPages * pageSize;
        int stackSize = max(totalMemory/512,ARG_MAX+65536);
        int stackPages = 0;
        if(totalPages > 1) {
            stackSize = max(stackSize,pageSize);
            stackSize = (stackSize + pageSize - 1) & ~(pageSize-1);
            stackPages = stackSize >>> pageShift;
            heapStart = (heapStart + pageSize - 1) & ~(pageSize-1);
            if(stackPages + STACK_GUARD_PAGES + (heapStart >>> pageShift) >= totalPages)
                throw new IllegalArgumentException("total pages too small");
        } else {
            if(pageSize < heapStart + stackSize) throw new IllegalArgumentException("total memory too small");
            heapStart = (heapStart + 4095) & ~4096;
        }
        
        stackBottom = totalMemory - stackSize;
        heapEnd = heapStart;
        
        readPages = new int[totalPages][];
        writePages = new int[totalPages][];
        
        if(totalPages == 1) {
            readPages[0] = writePages[0] = new int[pageSize>>2];
        } else {
            for(int i=(stackBottom >>> pageShift);i<writePages.length;i++) {
                readPages[i] = writePages[i] = new int[pageSize>>2];
            }
        }

        if(!exec) {
            fds = new FD[OPEN_MAX];
            closeOnExec = new boolean[OPEN_MAX];
        
            InputStream stdin = win32Hacks ? new Win32ConsoleIS(System.in) : System.in;
            addFD(new TerminalFD(stdin));
            addFD(new TerminalFD(pw));
            addFD(new TerminalFD(pw));
        }
    }
    
    /** Copy everything from <i>src</i> to <i>addr</i> initializing uninitialized pages if required. 
       Newly initalized pages will be marked read-only if <i>ro</i> is set */
    protected final void initPages(int[] src, int addr, boolean ro) {
        int pageWords = (1<<pageShift)>>>2;
        int pageMask = (1<<pageShift) - 1;
        
        for(int i=0;i<src.length;) {
            int page = addr >>> pageShift;
            int start = (addr&pageMask)>>2;
            int elements = min(pageWords-start,src.length-i);
            if(readPages[page]==null) {
                initPage(page,ro);
            } else if(!ro) {
                if(writePages[page] == null) writePages[page] = readPages[page];
            }
            System.arraycopy(src,i,readPages[page],start,elements);
            i += elements;
            addr += elements*4;
        }
    }
    
    /** Initialize <i>words</i> of pages starting at <i>addr</i> to 0 */
    protected final void clearPages(int addr, int words) {
        int pageWords = (1<<pageShift)>>>2;
        int pageMask = (1<<pageShift) - 1;

        for(int i=0;i<words;) {
            int page = addr >>> pageShift;
            int start = (addr&pageMask)>>2;
            int elements = min(pageWords-start,words-i);
            if(readPages[page]==null) {
                readPages[page] = writePages[page] = new int[pageWords];
            } else {
                if(writePages[page] == null) writePages[page] = readPages[page];
                for(int j=start;j<start+elements;j++) writePages[page][j] = 0;
            }
            i += elements;
            addr += elements*4;
        }
    }
    
    /** Copies <i>length</i> bytes from the processes memory space starting at
        <i>addr</i> INTO a java byte array <i>a</i> */
    public final void copyin(int addr, byte[] buf, int count) throws ReadFaultException {
        int pageWords = (1<<pageShift)>>>2;
        int pageMask = pageWords - 1;

        int x=0;
        if(count == 0) return;
        if((addr&3)!=0) {
            int word = memRead(addr&~3);
            switch(addr&3) {
                case 1: buf[x++] = (byte)((word>>>16)&0xff); if(--count==0) break;
                case 2: buf[x++] = (byte)((word>>> 8)&0xff); if(--count==0) break;
                case 3: buf[x++] = (byte)((word>>> 0)&0xff); if(--count==0) break;
            }
            addr = (addr&~3)+4;
        }
        if((count&~3) != 0) {
            int c = count>>>2;
            int a = addr>>>2;
            while(c != 0) {
                int[] page = readPages[a >>> (pageShift-2)];
                if(page == null) throw new ReadFaultException(a<<2);
                int index = a&pageMask;
                int n = min(c,pageWords-index);
                for(int i=0;i<n;i++,x+=4) {
                    int word = page[index+i];
                    buf[x+0] = (byte)((word>>>24)&0xff); buf[x+1] = (byte)((word>>>16)&0xff);
                    buf[x+2] = (byte)((word>>> 8)&0xff); buf[x+3] = (byte)((word>>> 0)&0xff);                        
                }
                a += n; c -=n;
            }
            addr = a<<2; count &=3;
        }
        if(count != 0) {
            int word = memRead(addr);
            switch(count) {
                case 3: buf[x+2] = (byte)((word>>>8)&0xff);
                case 2: buf[x+1] = (byte)((word>>>16)&0xff);
                case 1: buf[x+0] = (byte)((word>>>24)&0xff);
            }
        }
    }
    
    /** Copies <i>length</i> bytes OUT OF the java array <i>a</i> into the processes memory
        space at <i>addr</i> */
    public final void copyout(byte[] buf, int addr, int count) throws FaultException {
        int pageWords = (1<<pageShift)>>>2;
        int pageWordMask = pageWords - 1;
        
        int x=0;
        if(count == 0) return;
        if((addr&3)!=0) {
            int word = memRead(addr&~3);
            switch(addr&3) {
                case 1: word = (word&0xff00ffff)|((buf[x++]&0xff)<<16); if(--count==0) break;
                case 2: word = (word&0xffff00ff)|((buf[x++]&0xff)<< 8); if(--count==0) break;
                case 3: word = (word&0xffffff00)|((buf[x++]&0xff)<< 0); if(--count==0) break;
            }
            memWrite(addr&~3,word);
            addr += x;
        }

        if((count&~3) != 0) {
            int c = count>>>2;
            int a = addr>>>2;
            while(c != 0) {
                int[] page = writePages[a >>> (pageShift-2)];
                if(page == null) throw new WriteFaultException(a<<2);
                int index = a&pageWordMask;
                int n = min(c,pageWords-index);
                for(int i=0;i<n;i++,x+=4)
                    page[index+i] = ((buf[x+0]&0xff)<<24)|((buf[x+1]&0xff)<<16)|((buf[x+2]&0xff)<<8)|((buf[x+3]&0xff)<<0);
                a += n; c -=n;
            }
            addr = a<<2; count&=3;
        }

        if(count != 0) {
            int word = memRead(addr);
            switch(count) {
                case 1: word = (word&0x00ffffff)|((buf[x+0]&0xff)<<24); break;
                case 2: word = (word&0x0000ffff)|((buf[x+0]&0xff)<<24)|((buf[x+1]&0xff)<<16); break;
                case 3: word = (word&0x000000ff)|((buf[x+0]&0xff)<<24)|((buf[x+1]&0xff)<<16)|((buf[x+2]&0xff)<<8); break;
            }
            memWrite(addr,word);
        }
    }
    
    public final void memcpy(int dst, int src, int count) throws FaultException {
        int pageWords = (1<<pageShift)>>>2;
        int pageWordMask = pageWords - 1;
        if((dst&3) == 0 && (src&3)==0) {
            if((count&~3) != 0) {
                int c = count>>2;
                int s = src>>>2;
                int d = dst>>>2;
                while(c != 0) {
                    int[] srcPage = readPages[s>>>(pageShift-2)];
                    if(srcPage == null) throw new ReadFaultException(s<<2);
                    int[] dstPage = writePages[d>>>(pageShift-2)];
                    if(dstPage == null) throw new WriteFaultException(d<<2);
                    int srcIndex = s&pageWordMask;
                    int dstIndex = d&pageWordMask;
                    int n = min(c,pageWords-max(srcIndex,dstIndex));
                    System.arraycopy(srcPage,srcIndex,dstPage,dstIndex,n);
                    s += n; d += n; c -= n;
                }
                src = s<<2; dst = d<<2; count&=3;
            }
            if(count != 0) {
                int word1 = memRead(src);
                int word2 = memRead(dst);
                switch(count) {
                    case 1: memWrite(dst,(word1&0xff000000)|(word2&0x00ffffff)); break;
                    case 2: memWrite(dst,(word1&0xffff0000)|(word2&0x0000ffff)); break;
                    case 3: memWrite(dst,(word1&0xffffff00)|(word2&0x000000ff)); break;
                }
            }
        } else {
            while(count > 0) {
                int n = min(count,MAX_CHUNK);
                byte[] buf = byteBuf(n);
                copyin(src,buf,n);
                copyout(buf,dst,n);
                count -= n; src += n; dst += n;
            }
        }
    }
    
    public final void memset(int addr, int ch, int count) throws FaultException {
        int pageWords = (1<<pageShift)>>>2;
        int pageWordMask = pageWords - 1;
        
        int fourBytes = ((ch&0xff)<<24)|((ch&0xff)<<16)|((ch&0xff)<<8)|((ch&0xff)<<0);
        if((addr&3)!=0) {
            int word = memRead(addr&~3);
            switch(addr&3) {
                case 1: word = (word&0xff00ffff)|((ch&0xff)<<16); if(--count==0) break;
                case 2: word = (word&0xffff00ff)|((ch&0xff)<< 8); if(--count==0) break;
                case 3: word = (word&0xffffff00)|((ch&0xff)<< 0); if(--count==0) break;
            }
            memWrite(addr&~3,word);
            addr = (addr&~3)+4;
        }
        if((count&~3) != 0) {
            int c = count>>2;
            int a = addr>>>2;
            while(c != 0) {
                int[] page = readPages[a>>>(pageShift-2)];
                if(page == null) throw new WriteFaultException(a<<2);
                int index = a&pageWordMask;
                int n = min(c,pageWords-index);
                /* Arrays.fill(page,index,index+n,fourBytes);*/
                for(int i=index;i<index+n;i++) page[i] = fourBytes;
                a += n; c -= n;
            }
            addr = a<<2; count&=3;
        }
        if(count != 0) {
            int word = memRead(addr);
            switch(count) {
                case 1: word = (word&0x00ffffff)|(fourBytes&0xff000000); break;
                case 2: word = (word&0x0000ffff)|(fourBytes&0xffff0000); break;
                case 3: word = (word&0x000000ff)|(fourBytes&0xffffff00); break;
            }
            memWrite(addr,word);
        }
    }
    
    /** Read a word from the processes memory at <i>addr</i> */
    public final int memRead(int addr) throws ReadFaultException  {
        if((addr & 3) != 0) throw new ReadFaultException(addr);
        return unsafeMemRead(addr);
    }
       
    protected final int unsafeMemRead(int addr) throws ReadFaultException {
        int page = addr >>> pageShift;
        int entry = (addr&(1<<pageShift) - 1)>>2;
        try {
            return readPages[page][entry];
        } catch(ArrayIndexOutOfBoundsException e) {
            if(page < 0 || page >= readPages.length) throw new ReadFaultException(addr);
            throw e; // should never happen
        } catch(NullPointerException e) {
            throw new ReadFaultException(addr);
        }
    }
    
    /** Writes a word to the processes memory at <i>addr</i> */
    public final void memWrite(int addr, int value) throws WriteFaultException  {
        if((addr & 3) != 0) throw new WriteFaultException(addr);
        unsafeMemWrite(addr,value);
    }
    
    protected final void unsafeMemWrite(int addr, int value) throws WriteFaultException {
        int page = addr >>> pageShift;
        int entry = (addr&(1<<pageShift) - 1)>>2;
        try {
            writePages[page][entry] = value;
        } catch(ArrayIndexOutOfBoundsException e) {
            if(page < 0 || page >= writePages.length) throw new WriteFaultException(addr);
            throw e; // should never happen
        } catch(NullPointerException e) {
            throw new WriteFaultException(addr);
        }
    }
    
    /** Created a new non-empty writable page at page number <i>page</i> */
    private final int[] initPage(int page) { return initPage(page,false); }
    /** Created a new non-empty page at page number <i>page</i>. If <i>ro</i> is set the page will be read-only */
    private final int[] initPage(int page, boolean ro) {
        int[] buf = new int[(1<<pageShift)>>>2];
        writePages[page] = ro ? null : buf;
        readPages[page] = buf;
        return buf;
    }
    
    /** Returns the exit status of the process. (only valid if state == DONE) 
        @see Runtime#state */
    public final int exitStatus() {
        if(state != EXITED) throw new IllegalStateException("exitStatus() called in an inappropriate state");
        return exitStatus;
    }
        
    private int addStringArray(String[] strings, int topAddr) throws FaultException {
        int count = strings.length;
        int total = 0; /* null last table entry  */
        for(int i=0;i<count;i++) total += strings[i].length() + 1;
        total += (count+1)*4;
        int start = (topAddr - total)&~3;
        int addr = start + (count+1)*4;
        int[] table = new int[count+1];
        try {
            for(int i=0;i<count;i++) {
                byte[] a = getBytes(strings[i]);
                table[i] = addr;
                copyout(a,addr,a.length);
                memset(addr+a.length,0,1);
                addr += a.length + 1;
            }
            addr=start;
            for(int i=0;i<count+1;i++) {
                memWrite(addr,table[i]);
                addr += 4;
            }
        } catch(FaultException e) {
            throw new RuntimeException(e.toString());
        }
        return start;
    }
    
    String[] createEnv(String[] extra) { if(extra == null) extra = new String[0]; return extra; }
    
    /** Sets word number <i>index</i> in the _user_info table to <i>word</i>
     * The user_info table is a chunk of memory in the program's memory defined by the
     * symbol "user_info". The compiler/interpreter automatically determine the size
     * and location of the user_info table from the ELF symbol table. setUserInfo and
     * getUserInfo are used to modify the words in the user_info table. */
    public void setUserInfo(int index, int word) {
        if(index < 0 || index >= userInfoSize()/4) throw new IndexOutOfBoundsException("setUserInfo called with index >= " + (userInfoSize()/4));
        try {
            memWrite(userInfoBase()+index*4,word);
        } catch(FaultException e) { throw new RuntimeException(e.toString()); }
    }
    
    /** Returns the word in the _user_info table entry <i>index</i>
        @see Runtime#setUserInfo(int,int) setUserInfo */
    public int getUserInfo(int index) {
        if(index < 0 || index >= userInfoSize()/4) throw new IndexOutOfBoundsException("setUserInfo called with index >= " + (userInfoSize()/4));
        try {
            return memRead(userInfoBase()+index*4);
        } catch(FaultException e) { throw new RuntimeException(e.toString()); }
    }
    
    /** Calls _execute() (subclass's execute()) and catches exceptions */
    private void __execute() {
        try {
            _execute();
        } catch(FaultException e) {
            if(STDERR_DIAG) e.printStackTrace();
            exit(128+11,true); // SIGSEGV
            exitException = e;
        } catch(ExecutionException e) {
            if(STDERR_DIAG) e.printStackTrace();
            exit(128+4,true); // SIGILL
            exitException = e;
        }
    }
    
    /** Executes the process until the PAUSE syscall is invoked or the process exits. Returns true if the process exited. */
    public final boolean execute()  {
        if(state != PAUSED) throw new IllegalStateException("execute() called in inappropriate state");
        if(startTime == 0) startTime = System.currentTimeMillis();
        state = RUNNING;
        __execute();
        if(state != PAUSED && state != EXITED && state != EXECED)
            throw new IllegalStateException("execute() ended up in an inappropriate state (" + state + ")");
        return state != PAUSED;
    }
    
    static String[] concatArgv(String argv0, String[] rest) {
        String[] argv = new String[rest.length+1];
        System.arraycopy(rest,0,argv,1,rest.length);
        argv[0] = argv0;
        return argv;
    }
    
    public final int run() { return run(null); }
    public final int run(String argv0, String[] rest) { return run(concatArgv(argv0,rest)); }
    public final int run(String[] args) { return run(args,null); }
    
    /** Runs the process until it exits and returns the exit status.
        If the process executes the PAUSE syscall execution will be paused for 500ms and a warning will be displayed */
    public final int run(String[] args, String[] env) {
        start(args,env);
        for(;;) {
            if(execute()) break;
            if(STDERR_DIAG) System.err.println("WARNING: Pause requested while executing run()");
        }
        if(state == EXECED && STDERR_DIAG) System.err.println("WARNING: Process exec()ed while being run under run()");
        return state == EXITED ? exitStatus() : 0;
    }

    public final void start() { start(null); }
    public final void start(String[] args) { start(args,null); }
    
    /** Initializes the process and prepairs it to be executed with execute() */
    public final void start(String[] args, String[] environ)  {
        int top, sp, argsAddr, envAddr;
        if(state != STOPPED) throw new IllegalStateException("start() called in inappropriate state");
        if(args == null) args = new String[]{getClass().getName()};
        
        sp = top = writePages.length*(1<<pageShift);
        try {
            sp = argsAddr = addStringArray(args,sp);
            sp = envAddr = addStringArray(createEnv(environ),sp);
        } catch(FaultException e) {
            throw new IllegalArgumentException("args/environ too big");
        }
        sp &= ~15;
        if(top - sp > ARG_MAX) throw new IllegalArgumentException("args/environ too big");

        // HACK: heapStart() isn't always available when the constructor
        // is run and this sometimes doesn't get initialized
        if(heapEnd == 0) {
            heapEnd = heapStart();
            if(heapEnd == 0) throw new Error("heapEnd == 0");
            int pageSize = writePages.length == 1 ? 4096 : (1<<pageShift);
            heapEnd = (heapEnd + pageSize - 1) & ~(pageSize-1);
        }

        CPUState cpuState = new CPUState();
        cpuState.r[A0] = argsAddr;
        cpuState.r[A1] = envAddr;
        cpuState.r[SP] = sp;
        cpuState.r[RA] = 0xdeadbeef;
        cpuState.r[GP] = gp();
        cpuState.pc = entryPoint();
        setCPUState(cpuState);
        
        state = PAUSED;
        
        _started();        
    }

    public final void stop() {
        if (state != RUNNING && state != PAUSED) throw new IllegalStateException("stop() called in inappropriate state");
        exit(0, false);
    }

    /** Hook for subclasses to do their own startup */
    void _started() {  }
    
    public final int call(String sym, Object[] args) throws CallException, FaultException {
        if(state != PAUSED && state != CALLJAVA) throw new IllegalStateException("call() called in inappropriate state");
        if(args.length > 7) throw new IllegalArgumentException("args.length > 7");
        CPUState state = new CPUState();
        getCPUState(state);
        
        int sp = state.r[SP];
        int[] ia = new int[args.length];
        for(int i=0;i<args.length;i++) {
            Object o = args[i];
            byte[] buf = null;
            if(o instanceof String) {
                buf = getBytes((String)o);
            } else if(o instanceof byte[]) {
                buf = (byte[]) o;
            } else if(o instanceof Number) {
                ia[i] = ((Number)o).intValue();
            }
            if(buf != null) {
                sp -= buf.length;
                copyout(buf,sp,buf.length);
                ia[i] = sp;
            }
        }
        int oldSP = state.r[SP];
        if(oldSP == sp) return call(sym,ia);
        
        state.r[SP] = sp;
        setCPUState(state);
        int ret = call(sym,ia);
        state.r[SP] = oldSP;
        setCPUState(state);
        return ret;
    }
    
    public final int call(String sym) throws CallException { return call(sym,new int[]{}); }
    public final int call(String sym, int a0) throws CallException  { return call(sym,new int[]{a0}); }
    public final int call(String sym, int a0, int a1) throws CallException  { return call(sym,new int[]{a0,a1}); }
    
    /** Calls a function in the process with the given arguments */
    public final int call(String sym, int[] args) throws CallException {
        int func = lookupSymbol(sym);
        if(func == -1) throw new CallException(sym + " not found");
        int helper = lookupSymbol("_call_helper");
        if(helper == -1) throw new CallException("_call_helper not found");
        return call(helper,func,args);
    }
    
    /** Executes the code at <i>addr</i> in the process setting A0-A3 and S0-S3 to the given arguments
        and returns the contents of V1 when the the pause syscall is invoked */
    //public final int call(int addr, int a0, int a1, int a2, int a3, int s0, int s1, int s2, int s3) {
    public final int call(int addr, int a0, int[] rest) throws CallException {
        if(rest.length > 7) throw new IllegalArgumentException("rest.length > 7");
        if(state != PAUSED && state != CALLJAVA) throw new IllegalStateException("call() called in inappropriate state");
        int oldState = state;
        CPUState saved = new CPUState();        
        getCPUState(saved);
        CPUState cpustate = saved.dup();
        
        cpustate.r[SP] = cpustate.r[SP]&~15;
        cpustate.r[RA] = 0xdeadbeef;
        cpustate.r[A0] = a0;
        switch(rest.length) {            
            case 7: cpustate.r[S3] = rest[6];
            case 6: cpustate.r[S2] = rest[5];
            case 5: cpustate.r[S1] = rest[4];
            case 4: cpustate.r[S0] = rest[3];
            case 3: cpustate.r[A3] = rest[2];
            case 2: cpustate.r[A2] = rest[1];
            case 1: cpustate.r[A1] = rest[0];
        }
        cpustate.pc = addr;
        
        state = RUNNING;

        setCPUState(cpustate);
        __execute();
        getCPUState(cpustate);
        setCPUState(saved);

        if(state != PAUSED) throw new CallException("Process exit()ed while servicing a call() request");
        state = oldState;
        
        return cpustate.r[V1];
    }
        
    /** Allocated an entry in the FileDescriptor table for <i>fd</i> and returns the number.
        Returns -1 if the table is full. This can be used by subclasses to use custom file
        descriptors */
    public final int addFD(FD fd)
    {
        if(state == EXITED || state == EXECED) throw new IllegalStateException("addFD called in inappropriate state");
        int i;
        for(i=0;i<OPEN_MAX;i++) if(fds[i] == null) break;
        if(i==OPEN_MAX) return -1;
        fds[i] = fd;
        closeOnExec[i] = false;
        return i;
    }

    /** Hooks for subclasses before and after the process closes an FD */
    void _preCloseFD(FD fd) {  }
    void _postCloseFD(FD fd) {  }

    /** Closes file descriptor <i>fdn</i> and removes it from the file descriptor table */
    public final boolean closeFD(int fdn) {
        if(state == EXITED || state == EXECED) throw new IllegalStateException("closeFD called in inappropriate state");
        if(fdn < 0 || fdn >= OPEN_MAX) return false;
        if(fds[fdn] == null) return false;
        _preCloseFD(fds[fdn]);
        fds[fdn].close();
        _postCloseFD(fds[fdn]);
        fds[fdn] = null;        
        return true;
    }
    
    /** Duplicates the file descriptor <i>fdn</i> and returns the new fs */
    public final int dupFD(int fdn) {
        int i;
        if(fdn < 0 || fdn >= OPEN_MAX) return -1;
        if(fds[fdn] == null) return -1;
        for(i=0;i<OPEN_MAX;i++) if(fds[i] == null) break;
        if(i==OPEN_MAX) return -1;
        fds[i] = fds[fdn].dup();
        return i;
    }

    public static final int RD_ONLY = 0;
    public static final int WR_ONLY = 1;
    public static final int RDWR = 2;
    
    public static final int O_CREAT = 0x0200;
    public static final int O_EXCL = 0x0800;
    public static final int O_APPEND = 0x0008;
    public static final int O_TRUNC = 0x0400;
    public static final int O_NONBLOCK = 0x4000;
    public static final int O_NOCTTY = 0x8000;
    
    
    FD hostFSOpen(final File f, int flags, int mode, final Object data) throws ErrnoException {
        if((flags & ~(3|O_CREAT|O_EXCL|O_APPEND|O_TRUNC)) != 0) {
            if(STDERR_DIAG)
                System.err.println("WARNING: Unsupported flags passed to open(\"" + f + "\"): " + toHex(flags & ~(3|O_CREAT|O_EXCL|O_APPEND|O_TRUNC)));
            throw new ErrnoException(ENOTSUP);
        }
        boolean write = (flags&3) != RD_ONLY;

        if(sm != null && !(write ? sm.allowWrite(f) : sm.allowRead(f))) throw new ErrnoException(EACCES);
        
        if((flags & (O_EXCL|O_CREAT)) == (O_EXCL|O_CREAT)) {
            try {
                if(!Platform.atomicCreateFile(f)) throw new ErrnoException(EEXIST);
            } catch(IOException e) {
                throw new ErrnoException(EIO);
            }
        } else if(!f.exists()) {
            if((flags&O_CREAT)==0) return null;
        } else if(f.isDirectory()) {
            return hostFSDirFD(f,data);
        }
        
        final Seekable.File sf;
        try {
            sf = new Seekable.File(f,write,(flags & O_TRUNC) != 0);
        } catch(FileNotFoundException e) {
            if(e.getMessage() != null && e.getMessage().indexOf("Permission denied") >= 0) throw new ErrnoException(EACCES);
            return null;
        } catch(IOException e) { throw new ErrnoException(EIO); }
        
        return new SeekableFD(sf,flags) { protected FStat _fstat() { return hostFStat(f,sf,data); } };
    }
    
    FStat hostFStat(File f, Seekable.File sf, Object data) { return new HostFStat(f,sf); }
    
    FD hostFSDirFD(File f, Object data) { return null; }
    
    FD _open(String path, int flags, int mode) throws ErrnoException {
        return hostFSOpen(new File(path),flags,mode,null);
    }
    
    /** The open syscall */
    private int sys_open(int addr, int flags, int mode) throws ErrnoException, FaultException {
        String name = cstring(addr);
        
        // HACK: TeX, or GPC, or something really sucks
        if(name.length() == 1024 && getClass().getName().equals("tests.TeX")) name = name.trim();
        
        flags &= ~O_NOCTTY; // this is meaningless under nestedvm
        FD fd = _open(name,flags,mode);
        if(fd == null) return -ENOENT;
        int fdn = addFD(fd);
        if(fdn == -1) { fd.close(); return -ENFILE; }
        return fdn;
    }

    /** The write syscall */
    
    private int sys_write(int fdn, int addr, int count) throws FaultException, ErrnoException {
        count = Math.min(count,MAX_CHUNK);
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        byte[] buf = byteBuf(count);
        copyin(addr,buf,count);
        try {
            return fds[fdn].write(buf,0,count);
        } catch(ErrnoException e) {
            if(e.errno == EPIPE) sys_exit(128+13);
            throw e;
        }
    }

    /** The read syscall */
    private int sys_read(int fdn, int addr, int count) throws FaultException, ErrnoException {
        count = Math.min(count,MAX_CHUNK);
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        byte[] buf = byteBuf(count);
        int n = fds[fdn].read(buf,0,count);
        copyout(buf,addr,n);
        return n;
    }

    /** The ftruncate syscall */
    private int sys_ftruncate(int fdn, long length) {
      if (fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
      if (fds[fdn] == null) return -EBADFD;

      Seekable seekable = fds[fdn].seekable();
      if (length < 0 || seekable == null) return -EINVAL;
      try { seekable.resize(length); } catch (IOException e) { return -EIO; }
      return 0;
    }
    
    /** The close syscall */
    private int sys_close(int fdn) {
        return closeFD(fdn) ? 0 : -EBADFD;
    }

    
    /** The seek syscall */
    private int sys_lseek(int fdn, int offset, int whence) throws ErrnoException {
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        if(whence != SEEK_SET && whence !=  SEEK_CUR && whence !=  SEEK_END) return -EINVAL;
        int n = fds[fdn].seek(offset,whence);
        return n < 0 ? -ESPIPE : n;
    }
    
    /** The stat/fstat syscall helper */
    int stat(FStat fs, int addr) throws FaultException {
        memWrite(addr+0,(fs.dev()<<16)|(fs.inode()&0xffff)); // st_dev (top 16), // st_ino (bottom 16)
        memWrite(addr+4,((fs.type()&0xf000))|(fs.mode()&0xfff)); // st_mode
        memWrite(addr+8,fs.nlink()<<16|fs.uid()&0xffff); // st_nlink (top 16) // st_uid (bottom 16)
        memWrite(addr+12,fs.gid()<<16|0); // st_gid (top 16) // st_rdev (bottom 16)
        memWrite(addr+16,fs.size()); // st_size
        memWrite(addr+20,fs.atime()); // st_atime
        // memWrite(addr+24,0) // st_spare1
        memWrite(addr+28,fs.mtime()); // st_mtime
        // memWrite(addr+32,0) // st_spare2
        memWrite(addr+36,fs.ctime()); // st_ctime
        // memWrite(addr+40,0) // st_spare3
        memWrite(addr+44,fs.blksize()); // st_bklsize;
        memWrite(addr+48,fs.blocks()); // st_blocks
        // memWrite(addr+52,0) // st_spare4[0]
        // memWrite(addr+56,0) // st_spare4[1]
        return 0;
    }
    
    /** The fstat syscall */
    private int sys_fstat(int fdn, int addr) throws FaultException {
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        return stat(fds[fdn].fstat(),addr);
    }
    
    /*
    struct timeval {
    long tv_sec;
    long tv_usec;
    };
    */
    private int sys_gettimeofday(int timevalAddr, int timezoneAddr) throws FaultException {
        long now = System.currentTimeMillis();
        int tv_sec = (int)(now / 1000);
        int tv_usec = (int)((now%1000)*1000);
        memWrite(timevalAddr+0,tv_sec);
        memWrite(timevalAddr+4,tv_usec);
        return 0;
    }
    
    private int sys_sleep(int sec) {
        if(sec < 0) sec = Integer.MAX_VALUE;
        try {
            Thread.sleep((long)sec*1000);
            return 0;
        } catch(InterruptedException e) {
            return -1;
        }
    }
    
    /*
      #define _CLOCKS_PER_SEC_ 1000
      #define    _CLOCK_T_    unsigned long
    struct tms {
      clock_t   tms_utime;
      clock_t   tms_stime;
      clock_t   tms_cutime;    
      clock_t   tms_cstime;
    };*/
   
    private int sys_times(int tms) {
        long now = System.currentTimeMillis();
        int userTime = (int)((now - startTime)/16);
        int sysTime = (int)((now - startTime)/16);
        
        try {
            if(tms!=0) {
                memWrite(tms+0,userTime);
                memWrite(tms+4,sysTime);
                memWrite(tms+8,userTime);
                memWrite(tms+12,sysTime);
            }
        } catch(FaultException e) {
            return -EFAULT;
        }
        return (int)now;
    }
    
    private int sys_sysconf(int n) {
        switch(n) {
            case _SC_CLK_TCK: return 1000;
            case _SC_PAGESIZE: return  writePages.length == 1 ? 4096 : (1<<pageShift);
            case _SC_PHYS_PAGES: return writePages.length == 1 ? (1<<pageShift)/4096 : writePages.length;
            default:
                if(STDERR_DIAG) System.err.println("WARNING: Attempted to use unknown sysconf key: " + n);
                return -EINVAL;
        }
    }
    
    /** The sbrk syscall. This can also be used by subclasses to allocate memory.
        <i>incr</i> is how much to increase the break by */
    public final int sbrk(int incr) {
        if(incr < 0) return -ENOMEM;
        if(incr==0) return heapEnd;
        incr = (incr+3)&~3;
        int oldEnd = heapEnd;
        int newEnd = oldEnd + incr;
        if(newEnd >= stackBottom) return -ENOMEM;
        
        if(writePages.length > 1) {
            int pageMask = (1<<pageShift) - 1;
            int pageWords = (1<<pageShift) >>> 2;
            int start = (oldEnd + pageMask) >>> pageShift;
            int end = (newEnd + pageMask) >>> pageShift;
            try {
                for(int i=start;i<end;i++) readPages[i] = writePages[i] = new int[pageWords];
            } catch(OutOfMemoryError e) {
                if(STDERR_DIAG) System.err.println("WARNING: Caught OOM Exception in sbrk: " + e);
                return -ENOMEM;
            }
        }
        heapEnd = newEnd;
        return oldEnd;
    }

    /** The getpid syscall */
    private int sys_getpid() { return getPid(); }
    int getPid() { return 1; }
    
    public static interface CallJavaCB { public int call(int a, int b, int c, int d); }
    
    private int sys_calljava(int a, int b, int c, int d) {
        if(state != RUNNING) throw new IllegalStateException("wound up calling sys_calljava while not in RUNNING");
        if(callJavaCB != null) {
            state = CALLJAVA;
            int ret;
            try {
                ret = callJavaCB.call(a,b,c,d);
            } catch(RuntimeException e) {
                System.err.println("Error while executing callJavaCB");
                e.printStackTrace();
                ret = 0;
            }
            state = RUNNING;
            return ret;
        } else {
            if(STDERR_DIAG) System.err.println("WARNING: calljava syscall invoked without a calljava callback set");
            return 0;
        }
    }
        
    private int sys_pause() {
        state = PAUSED;
        return 0;
    }
    
    private int sys_getpagesize() { return writePages.length == 1 ? 4096 : (1<<pageShift); }
    
    /** Hook for subclasses to do something when the process exits  */
    void _exited() {  }
    
    void exit(int status, boolean fromSignal) {
        if(fromSignal && fds[2] != null) {
            try {
                byte[] msg = getBytes("Process exited on signal " + (status - 128) + "\n");
                fds[2].write(msg,0,msg.length);
            } catch(ErrnoException e) { }
        }
        exitStatus = status;
        for(int i=0;i<fds.length;i++) if(fds[i] != null) closeFD(i);
        state = EXITED;
        _exited();
    }
    
    private int sys_exit(int status) {
        exit(status,false);
        return 0;
    }
       
    final int sys_fcntl(int fdn, int cmd, int arg) throws FaultException {
        int i;
            
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        FD fd = fds[fdn];
        
        switch(cmd) {
            case F_DUPFD:
                if(arg < 0 || arg >= OPEN_MAX) return -EINVAL;
                for(i=arg;i<OPEN_MAX;i++) if(fds[i]==null) break;
                if(i==OPEN_MAX) return -EMFILE;
                fds[i] = fd.dup();
                return i;
            case F_GETFL:
                return fd.flags();
            case F_SETFD:
                closeOnExec[fdn] = arg != 0;
                return 0;
            case F_GETFD:
                return closeOnExec[fdn] ? 1 : 0;
            case F_GETLK:
            case F_SETLK:
                if(STDERR_DIAG) System.err.println("WARNING: file locking requires UnixRuntime");
                return -ENOSYS;
            default:
                if(STDERR_DIAG) System.err.println("WARNING: Unknown fcntl command: " + cmd);
                return -ENOSYS;
        }
    }

    final int fsync(int fdn) {
        if(fdn < 0 || fdn >= OPEN_MAX) return -EBADFD;
        if(fds[fdn] == null) return -EBADFD;
        FD fd = fds[fdn];

        Seekable s = fd.seekable();
        if (s == null) return -EINVAL;

        try {
            s.sync();
            return 0;
        } catch (IOException e) {
            return -EIO;
        }
    }

    /** The syscall dispatcher.
        The should be called by subclasses when the syscall instruction is invoked.
        <i>syscall</i> should be the contents of V0 and <i>a</i>, <i>b</i>, <i>c</i>, and <i>d</i> should be 
        the contenst of A0, A1, A2, and A3. The call MAY change the state
        @see Runtime#state state */
    protected final int syscall(int syscall, int a, int b, int c, int d, int e, int f) {
        try {
            int n = _syscall(syscall,a,b,c,d,e,f);
            //if(n<0) throw new ErrnoException(-n);
            return n;
        } catch(ErrnoException ex) {
            //System.err.println("While executing syscall: " + syscall + ":");
            //if(syscall == SYS_open) try { System.err.println("Failed to open " + cstring(a) + " errno " + ex.errno); } catch(Exception e2) { }
            //ex.printStackTrace();
            return -ex.errno;
        } catch(FaultException ex) {
            return -EFAULT;
        } catch(RuntimeException ex) {
            ex.printStackTrace();
            throw new Error("Internal Error in _syscall()");
        }
    }
    
    int _syscall(int syscall, int a, int b, int c, int d, int e, int f) throws ErrnoException, FaultException {
        switch(syscall) {
            case SYS_null: return 0;
            case SYS_exit: return sys_exit(a);
            case SYS_pause: return sys_pause();
            case SYS_write: return sys_write(a,b,c);
            case SYS_fstat: return sys_fstat(a,b);
            case SYS_sbrk: return sbrk(a);
            case SYS_open: return sys_open(a,b,c);
            case SYS_close: return sys_close(a);
            case SYS_read: return sys_read(a,b,c);
            case SYS_lseek: return sys_lseek(a,b,c);
            case SYS_ftruncate: return sys_ftruncate(a,b);
            case SYS_getpid: return sys_getpid();
            case SYS_calljava: return sys_calljava(a,b,c,d);
            case SYS_gettimeofday: return sys_gettimeofday(a,b);
            case SYS_sleep: return sys_sleep(a);
            case SYS_times: return sys_times(a);
            case SYS_getpagesize: return sys_getpagesize();
            case SYS_fcntl: return sys_fcntl(a,b,c);
            case SYS_sysconf: return sys_sysconf(a);
            case SYS_getuid: return sys_getuid();
            case SYS_geteuid: return sys_geteuid();
            case SYS_getgid: return sys_getgid();
            case SYS_getegid: return sys_getegid();
            
            case SYS_fsync: return fsync(a);
            case SYS_memcpy: memcpy(a,b,c); return a;
            case SYS_memset: memset(a,b,c); return a;

            case SYS_kill:
            case SYS_fork:
            case SYS_pipe:
            case SYS_dup2:
            case SYS_waitpid:
            case SYS_stat:
            case SYS_mkdir:
            case SYS_getcwd:
            case SYS_chdir:
                if(STDERR_DIAG) System.err.println("Attempted to use a UnixRuntime syscall in Runtime (" + syscall + ")");
                return -ENOSYS;
            default:
                if(STDERR_DIAG) System.err.println("Attempted to use unknown syscall: " + syscall);
                return -ENOSYS;
        }
    }
    
    private int sys_getuid() { return 0; }
    private int sys_geteuid() { return 0; }
    private int sys_getgid() { return 0; }
    private int sys_getegid() { return 0; }
    
    public int xmalloc(int size) { int p=malloc(size); if(p==0) throw new RuntimeException("malloc() failed"); return p; }
    public int xrealloc(int addr,int newsize) { int p=realloc(addr,newsize); if(p==0) throw new RuntimeException("realloc() failed"); return p; }
    public int realloc(int addr, int newsize) { try { return call("realloc",addr,newsize); } catch(CallException e) { return 0; } }
    public int malloc(int size) { try { return call("malloc",size); } catch(CallException e) { return 0; } }
    public void free(int p) { try { if(p!=0) call("free",p); } catch(CallException e) { /*noop*/ } }
    
    /** Helper function to create a cstring in main memory */
    public int strdup(String s) {
        byte[] a;
        if(s == null) s = "(null)";
        byte[] a2 = getBytes(s);
        a = new byte[a2.length+1];
        System.arraycopy(a2,0,a,0,a2.length);
        int addr = malloc(a.length);
        if(addr == 0) return 0;
        try {
            copyout(a,addr,a.length);
        } catch(FaultException e) {
            free(addr);
            return 0;
        }
        return addr;
    }

    // TODO: less memory copying (custom utf-8 reader)
    //       or at least roll strlen() into copyin()
    public final String utfstring(int addr) throws ReadFaultException {
        if (addr == 0) return null;

        // determine length
        int i=addr;
        for(int word = 1; word != 0; i++) {
            word = memRead(i&~3);
            switch(i&3) {
                case 0: word = (word>>>24)&0xff; break;
                case 1: word = (word>>>16)&0xff; break;
                case 2: word = (word>>> 8)&0xff; break;
                case 3: word = (word>>> 0)&0xff; break;
            }
        }
        if (i > addr) i--; // do not count null

        byte[] bytes = new byte[i-addr];
        copyin(addr, bytes, bytes.length);

        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // should never happen with UTF-8
        }
    }

    /** Helper function to read a cstring from main memory */
    public final String cstring(int addr) throws ReadFaultException {
        if (addr == 0) return null;
        StringBuffer sb = new StringBuffer();
        for(;;) {
            int word = memRead(addr&~3);
            switch(addr&3) {
                case 0: if(((word>>>24)&0xff)==0) return sb.toString(); sb.append((char)((word>>>24)&0xff)); addr++;
                case 1: if(((word>>>16)&0xff)==0) return sb.toString(); sb.append((char)((word>>>16)&0xff)); addr++;
                case 2: if(((word>>> 8)&0xff)==0) return sb.toString(); sb.append((char)((word>>> 8)&0xff)); addr++;
                case 3: if(((word>>> 0)&0xff)==0) return sb.toString(); sb.append((char)((word>>> 0)&0xff)); addr++;
            }
        }
    }
    
    /** File Descriptor class */
    public static abstract class FD {
        private int refCount = 1;
        private String normalizedPath = null;
        private boolean deleteOnClose = false;

        public void setNormalizedPath(String path) { normalizedPath = path; }
        public String getNormalizedPath() { return normalizedPath; }

        public void markDeleteOnClose() { deleteOnClose = true; }
        public boolean isMarkedForDeleteOnClose() { return deleteOnClose; }
        
        /** Read some bytes. Should return the number of bytes read, 0 on EOF, or throw an IOException on error */
        public int read(byte[] a, int off, int length) throws ErrnoException { throw new ErrnoException(EBADFD); }
        /** Write. Should return the number of bytes written or throw an IOException on error */
        public int write(byte[] a, int off, int length) throws ErrnoException { throw new ErrnoException(EBADFD); }

        /** Seek in the filedescriptor. Whence is SEEK_SET, SEEK_CUR, or SEEK_END. Should return -1 on error or the new position. */
        public int seek(int n, int whence)  throws ErrnoException  { return -1; }
        
        public int getdents(byte[] a, int off, int length) throws ErrnoException { throw new ErrnoException(EBADFD); }
        
        /** Return a Seekable object representing this file descriptor (can be read only) 
            This is required for exec() */
        Seekable seekable() { return null; }
        
        private FStat cachedFStat = null;
        public final FStat fstat() {
            if(cachedFStat == null) cachedFStat = _fstat(); 
            return cachedFStat;
        }
        
        protected abstract FStat _fstat();
        public abstract int  flags();
        
        /** Closes the fd */
        public final void close() { if(--refCount==0) _close(); }
        protected void _close() { /* noop*/ }
        
        FD dup() { refCount++; return this; }
    }
        
    /** FileDescriptor class for normal files */
    public abstract static class SeekableFD extends FD {
        private final int flags;
        private final Seekable data;
        
        SeekableFD(Seekable data, int flags) { this.data = data; this.flags = flags; }
        
        protected abstract FStat _fstat();
        public int flags() { return flags; }

        Seekable seekable() { return data; }
        
        public int seek(int n, int whence) throws ErrnoException {
            try {
                switch(whence) {
                        case SEEK_SET: break;
                        case SEEK_CUR: n += data.pos(); break;
                        case SEEK_END: n += data.length(); break;
                        default: return -1;
                }
                data.seek(n);
                return n;
            } catch(IOException e) {
                throw new ErrnoException(ESPIPE);
            }
        }
        
        public int write(byte[] a, int off, int length) throws ErrnoException {
            if((flags&3) == RD_ONLY) throw new ErrnoException(EBADFD);
            // NOTE: There is race condition here but we can't fix it in pure java
            if((flags&O_APPEND) != 0) seek(0,SEEK_END);
            try {
                return data.write(a,off,length);
            } catch(IOException e) {
                throw new ErrnoException(EIO);
            }
        }
        
        public int read(byte[] a, int off, int length) throws ErrnoException {
            if((flags&3) == WR_ONLY) throw new ErrnoException(EBADFD);
            try {
                int n = data.read(a,off,length);
                return n < 0 ? 0 : n;
            } catch(IOException e) {
                throw new ErrnoException(EIO);
            }
        }
        
        protected void _close() { try { data.close(); } catch(IOException e) { /*ignore*/ } }        
    }
    
    public static class InputOutputStreamFD extends FD {
        private final InputStream is;
        private final OutputStream os;
        
        public InputOutputStreamFD(InputStream is) { this(is,null); }
        public InputOutputStreamFD(OutputStream os) { this(null,os); }
        public InputOutputStreamFD(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            if(is == null && os == null) throw new IllegalArgumentException("at least one stream must be supplied");
        }
        
        public int flags() {
            if(is != null && os != null) return O_RDWR;
            if(is != null) return O_RDONLY;
            if(os != null) return O_WRONLY;
            throw new Error("should never happen");
        }
        
        public void _close() {
            if(is != null) try { is.close(); } catch(IOException e) { /*ignore*/ }
            if(os != null) try { os.close(); } catch(IOException e) { /*ignore*/ }
        }
        
        public int read(byte[] a, int off, int length) throws ErrnoException {
            if(is == null) return super.read(a,off,length);
            try {
                int n = is.read(a,off,length);
                return n < 0 ? 0 : n;
            } catch(IOException e) {
                throw new ErrnoException(EIO);
            }
        }    
        
        public int write(byte[] a, int off, int length) throws ErrnoException {
            if(os == null) return super.write(a,off,length);
            try {
                os.write(a,off,length);
                return length;
            } catch(IOException e) {
                throw new ErrnoException(EIO);
            }
        }
        
        public FStat _fstat() { return new SocketFStat(); }
    }
    
    static class TerminalFD extends InputOutputStreamFD {
        public TerminalFD(InputStream is) { this(is,null); }
        public TerminalFD(OutputStream os) { this(null,os); }
        public TerminalFD(InputStream is, OutputStream os) { super(is,os); }
        public void _close() { /* noop */ }
        public FStat _fstat() { return new SocketFStat() { public int type() { return S_IFCHR; } public int mode() { return 0600; } }; }
    }
    
    // This is pretty inefficient but it is only used for reading from the console on win32
    static class Win32ConsoleIS extends InputStream {
        private int pushedBack = -1;
        private final InputStream parent;
        public Win32ConsoleIS(InputStream parent) { this.parent = parent; }
        public int read() throws IOException {
            if(pushedBack != -1) { int c = pushedBack; pushedBack = -1; return c; }
            int c = parent.read();
            if(c == '\r' && (c = parent.read()) != '\n') { pushedBack = c; return '\r'; }
            return c;
        }
        public int read(byte[] buf, int pos, int len) throws IOException {
            boolean pb = false;
            if(pushedBack != -1 && len > 0) {
                buf[0] = (byte) pushedBack;
                pushedBack = -1;
                pos++; len--; pb = true;
            }
            int n = parent.read(buf,pos,len);
            if(n == -1) return pb ? 1 : -1;
            for(int i=0;i<n;i++) {
                if(buf[pos+i] == '\r') {
                    if(i==n-1) {
                        int c = parent.read();
                        if(c == '\n') buf[pos+i] = '\n';
                        else pushedBack = c;
                    } else if(buf[pos+i+1] == '\n') {
                        System.arraycopy(buf,pos+i+1,buf,pos+i,len-i-1);
                        n--;
                    }
                }
            }
            return n + (pb ? 1 : 0);
        }
    }
    
    public abstract static class FStat {
        public static final int S_IFIFO =  0010000;
        public static final int S_IFCHR =  0020000;
        public static final int S_IFDIR =  0040000;
        public static final int S_IFREG =  0100000;
        public static final int S_IFSOCK = 0140000;
        
        public int mode() { return 0; }
        public int nlink() { return 0; }
        public int uid() { return 0; }
        public int gid() { return 0; }
        public int size() { return 0; }
        public int atime() { return 0; }
        public int mtime() { return 0; }
        public int ctime() { return 0; }
        public int blksize() { return 512; }
        public int blocks() { return (size()+blksize()-1)/blksize(); }        
        
        public abstract int dev();
        public abstract int type();
        public abstract int inode();
    }
    
    public static class SocketFStat extends FStat {
        public int dev() { return -1; }
        public int type() { return S_IFSOCK; }
        public int inode() { return hashCode() & 0x7fff; }
    }
    
    static class HostFStat extends FStat {
        private final File f;
        private final Seekable.File sf;
        private final boolean executable; 
        public HostFStat(File f, Seekable.File sf) { this(f,sf,false); }
        public HostFStat(File f, boolean executable) {this(f,null,executable);}
        public HostFStat(File f, Seekable.File sf, boolean executable) {
            this.f = f;
            this.sf = sf;
            this.executable = executable;
        }
        public int dev() { return 1; }
        public int inode() { return f.getAbsolutePath().hashCode() & 0x7fff; }
        public int type() { return f.isDirectory() ? S_IFDIR : S_IFREG; }
        public int nlink() { return 1; }
        public int mode() {
            int mode = 0;
            boolean canread = f.canRead();
            if(canread && (executable || f.isDirectory())) mode |= 0111;
            if(canread) mode |= 0444;
            if(f.canWrite()) mode |= 0222;
            return mode;
        }
        public int size() {
          try {
            return sf != null ? (int)sf.length() : (int)f.length();
          } catch (Exception x) {
            return (int)f.length();
          }
        }
        public int mtime() { return (int)(f.lastModified()/1000); }        
    }
    
    // Exceptions
    public static class ReadFaultException extends FaultException {
        public ReadFaultException(int addr) { super(addr); }
    }
    public static class WriteFaultException extends FaultException {
        public WriteFaultException(int addr) { super(addr); }
    }
    public static class FaultException extends ExecutionException {
        public final int addr;
        public final RuntimeException cause;
        public FaultException(int addr) { super("fault at: " + toHex(addr)); this.addr = addr; cause = null; }
        public FaultException(RuntimeException e) { super(e.toString()); addr = -1; cause = e; }
    }
    public static class ExecutionException extends Exception {
        private String message = "(null)";
        private String location = "(unknown)";
        public ExecutionException() { /* noop */ }
        public ExecutionException(String s) { if(s != null) message = s; }
        void setLocation(String s) { location = s == null ? "(unknown)" : s; }
        public final String getMessage() { return message + " at " + location; }
    }
    public static class CallException extends Exception {
        public CallException(String s) { super(s); }
    }
    
    protected static class ErrnoException extends Exception {
        public int errno;
        public ErrnoException(int errno) { super("Errno: " + errno); this.errno = errno; }
    }
    
    // CPU State
    public static class CPUState {
        public CPUState() { /* noop */ }
        /* GPRs */
        public int[] r = new int[32];
        /* Floating point regs */
        public int[] f = new int[32];
        public int hi, lo;
        public int fcsr;
        public int pc;
        
        public CPUState dup() {
            CPUState c = new CPUState();
            c.hi = hi;
            c.lo = lo;
            c.fcsr = fcsr;
            c.pc = pc;
            for(int i=0;i<32;i++) {
                    c.r[i] = r[i];
                c.f[i] = f[i];
            }
            return c;
        }
    }
    
    public static class SecurityManager {
        public boolean allowRead(File f) { return true; }
        public boolean allowWrite(File f) { return true; }
        public boolean allowStat(File f) { return true; }
        public boolean allowUnlink(File f) { return true; }
    }
    
    // Null pointer check helper function
    protected final void nullPointerCheck(int addr) throws ExecutionException {
        if(addr < 65536)
            throw new ExecutionException("Attempted to dereference a null pointer " + toHex(addr));
    }
    
    // Utility functions
    byte[] byteBuf(int size) {
        if(_byteBuf==null) _byteBuf = new byte[size];
        else if(_byteBuf.length < size)
            _byteBuf = new byte[min(max(_byteBuf.length*2,size),MAX_CHUNK)];
        return _byteBuf;
    }
    
    /** Decode a packed string */
    protected static final int[] decodeData(String s, int words) {
        if(s.length() % 8 != 0) throw new IllegalArgumentException("string length must be a multiple of 8");
        if((s.length() / 8) * 7 < words*4) throw new IllegalArgumentException("string isn't big enough");
        int[] buf = new int[words];
        int prev = 0, left=0;
        for(int i=0,n=0;n<words;i+=8) {
            long l = 0;
            for(int j=0;j<8;j++) { l <<= 7; l |= s.charAt(i+j) & 0x7f; }
            if(left > 0) buf[n++] = prev | (int)(l>>>(56-left));
            if(n < words) buf[n++] = (int) (l >>> (24-left));
            left = (left + 8) & 0x1f;
            prev = (int)(l << left);
        }
        return buf;
    }
    
    static byte[] getBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch(UnsupportedEncodingException e) {
            return null; // should never happen
        }
    }
    
    static byte[] getNullTerminatedBytes(String s) {
        byte[] buf1 = getBytes(s);
        byte[] buf2 = new byte[buf1.length+1];
        System.arraycopy(buf1,0,buf2,0,buf1.length);
        return buf2;
    }
    
    final static String toHex(int n) { return "0x" + Long.toString(n & 0xffffffffL, 16); }
    final static int min(int a, int b) { return a < b ? a : b; }
    final static int max(int a, int b) { return a > b ? a : b; }

    public abstract static class Platform {
        Platform() { }
        private static final Platform p;

        static {
            float version;
            try {
                if(getProperty("java.vm.name").equals("SableVM"))
                    version = 1.2f;
                else
                    version = Float.valueOf(getProperty("java.specification.version")).floatValue();
            } catch(Exception e) {
                System.err.println("WARNING: " + e + " while trying to find jvm version -  assuming 1.1");
                version = 1.1f;
            }
            String platformClass;
            if(version >= 1.4f) platformClass = "Jdk14";
            else if(version >= 1.3f) platformClass = "Jdk13";
            else if(version >= 1.2f) platformClass = "Jdk12";
            else if(version >= 1.1f) platformClass = "Jdk11";
            else throw new Error("JVM Specification version: " + version + " is too old. (see org.ibex.util.Platform to add support)");

            try {
                p = (Platform) Class.forName(Platform.class.getName() + "$" + platformClass).newInstance();
            } catch(Exception e) {
                e.printStackTrace();
                throw new Error("Error instansiating platform class");
            }
        }

        public static String getProperty(String key) {
            try {
                return System.getProperty(key);
            } catch(SecurityException e) {
                return null;
            }
        }


        abstract boolean _atomicCreateFile(File f) throws IOException;
        public static boolean atomicCreateFile(File f) throws IOException { return p._atomicCreateFile(f); }

        abstract Seekable.Lock _lockFile(Seekable s, RandomAccessFile raf, long pos, long size, boolean shared) throws IOException;
        public static Seekable.Lock lockFile(Seekable s, RandomAccessFile raf, long pos, long size, boolean shared) throws IOException {
            return p._lockFile(s, raf, pos, size, shared); }

        abstract void _socketHalfClose(Socket s, boolean output) throws IOException;
        public static void socketHalfClose(Socket s, boolean output) throws IOException { p._socketHalfClose(s,output); }

        abstract void _socketSetKeepAlive(Socket s, boolean on) throws SocketException;
        public static void socketSetKeepAlive(Socket s, boolean on) throws SocketException { p._socketSetKeepAlive(s,on); }

        abstract InetAddress _inetAddressFromBytes(byte[] a) throws UnknownHostException;
        public static InetAddress inetAddressFromBytes(byte[] a) throws UnknownHostException { return p._inetAddressFromBytes(a); }

        abstract String _timeZoneGetDisplayName(TimeZone tz, boolean dst, boolean showlong, Locale l);
        public static String timeZoneGetDisplayName(TimeZone tz, boolean dst, boolean showlong, Locale l) { return p._timeZoneGetDisplayName(tz,dst,showlong,l); }
        public static String timeZoneGetDisplayName(TimeZone tz, boolean dst, boolean showlong) { return timeZoneGetDisplayName(tz,dst,showlong,Locale.getDefault()); }

        abstract void _setFileLength(RandomAccessFile f, int length)
            throws IOException;
        public static void setFileLength(RandomAccessFile f, int length)
            throws IOException { p._setFileLength(f, length); }

        abstract File[] _listRoots();
        public static File[] listRoots() { return p._listRoots(); }

        abstract File _getRoot(File f);
        public static File getRoot(File f) { return p._getRoot(f); }

        static class Jdk11 extends Platform {
            boolean _atomicCreateFile(File f) throws IOException {
                // This is not atomic, but its the best we can do on jdk 1.1
                if(f.exists()) return false;
                new FileOutputStream(f).close();
                return true;
            }
            Seekable.Lock _lockFile(Seekable s, RandomAccessFile raf, long p, long size, boolean shared) throws IOException {
                throw new IOException("file locking requires jdk 1.4+");
            }
            void _socketHalfClose(Socket s, boolean output) throws IOException {
                throw new IOException("half closing sockets not supported");
            }
            InetAddress _inetAddressFromBytes(byte[] a) throws UnknownHostException {
                if(a.length != 4) throw new UnknownHostException("only ipv4 addrs supported");
                return InetAddress.getByName(""+(a[0]&0xff)+"."+(a[1]&0xff)+"."+(a[2]&0xff)+"."+(a[3]&0xff));
            }
            void _socketSetKeepAlive(Socket s, boolean on) throws SocketException {
                if(on) throw new SocketException("keepalive not supported");
            }
            String _timeZoneGetDisplayName(TimeZone tz, boolean dst, boolean showlong, Locale l) {
                String[][] zs  = new DateFormatSymbols(l).getZoneStrings();
                String id = tz.getID();
                for(int i=0;i<zs.length;i++)
                    if(zs[i][0].equals(id))
                        return zs[i][dst ? (showlong ? 3 : 4) : (showlong ? 1 : 2)];
                StringBuffer sb = new StringBuffer("GMT");
                int off = tz.getRawOffset() / 1000;
                if(off < 0) { sb.append("-"); off = -off; }
                else sb.append("+");
                sb.append(off/3600); off = off%3600;
                if(off > 0) sb.append(":").append(off/60); off=off%60;
                if(off > 0) sb.append(":").append(off);
                return sb.toString();
            }

            void _setFileLength(RandomAccessFile f, int length) throws IOException{
                InputStream in = new FileInputStream(f.getFD());
                OutputStream out = new FileOutputStream(f.getFD());

                byte[] buf = new byte[1024];
                for (int len; length > 0; length -= len) {
                    len = in.read(buf, 0, Math.min(length, buf.length));
                    if (len == -1) break;
                    out.write(buf, 0, len);
                }
                if (length == 0) return;

                // fill the rest of the space with zeros
                for (int i=0; i < buf.length; i++) buf[i] = 0;
                while (length > 0) {
                    out.write(buf, 0, Math.min(length, buf.length));
                    length -= buf.length;
                }
            }

            RandomAccessFile _truncatedRandomAccessFile(File f, String mode) throws IOException {
                new FileOutputStream(f).close();
                return new RandomAccessFile(f,mode);
            }

            File[] _listRoots() {
                String[] rootProps = new String[]{"java.home","java.class.path","java.library.path","java.io.tmpdir","java.ext.dirs","user.home","user.dir" };
                Hashtable known = new Hashtable();
                for(int i=0;i<rootProps.length;i++) {
                    String prop = getProperty(rootProps[i]);
                    if(prop == null) continue;
                    for(;;) {
                        String path = prop;
                        int p;
                        if((p = prop.indexOf(File.pathSeparatorChar)) != -1) {
                            path = prop.substring(0,p);
                            prop = prop.substring(p+1);
                        }
                        File root = getRoot(new File(path));
                        //System.err.println(rootProps[i] + ": " + path + " -> " + root);
                        known.put(root,Boolean.TRUE);
                        if(p == -1) break;
                    }
                }
                File[] ret = new File[known.size()];
                int i=0;
                for(Enumeration e = known.keys(); e.hasMoreElements();)
                    ret[i++] = (File) e.nextElement();
                return ret;
            }

            File _getRoot(File f) {
                if(!f.isAbsolute()) f = new File(f.getAbsolutePath());
                String p;
                while((p = f.getParent()) != null) f = new File(p);
                if(f.getPath().length() == 0) f = new File("/"); // work around a classpath bug
                return f;
            }
        }

        static class Jdk12 extends Jdk11 {
            boolean _atomicCreateFile(File f) throws IOException {
                return f.createNewFile();
            }

            String _timeZoneGetDisplayName(TimeZone tz, boolean dst, boolean showlong, Locale l) {
                return tz.getDisplayName(dst,showlong ? TimeZone.LONG : TimeZone.SHORT, l);
            }

            void _setFileLength(RandomAccessFile f, int length) throws IOException {
                f.setLength(length);
            }

            File[] _listRoots() { return File.listRoots(); }
        }

        static class Jdk13 extends Jdk12 {
            void _socketHalfClose(Socket s, boolean output) throws IOException {
                if(output) s.shutdownOutput();
                else s.shutdownInput();
            }

            void _socketSetKeepAlive(Socket s, boolean on) throws SocketException {
                s.setKeepAlive(on);
            }
        }

        static class Jdk14 extends Jdk13 {
            InetAddress _inetAddressFromBytes(byte[] a) throws UnknownHostException { return InetAddress.getByAddress(a); }

            Seekable.Lock _lockFile(Seekable s, RandomAccessFile r, long pos, long size, boolean shared) throws IOException {
                FileLock flock;
                try {
                    flock = pos == 0 && size == 0 ? r.getChannel().lock() :
                        r.getChannel().tryLock(pos, size, shared);
                } catch (OverlappingFileLockException e) { flock = null; }
                if (flock == null) return null; // region already locked
                return new Jdk14FileLock(s, flock);
            }
        }

        private static final class Jdk14FileLock extends Seekable.Lock {
            private final Seekable s;
            private final FileLock l;

            Jdk14FileLock(Seekable sk, FileLock flock) { s = sk; l = flock; }
            public Seekable seekable() { return s; }
            public boolean isShared() { return l.isShared(); }
            public boolean isValid() { return l.isValid(); }
            public void release() throws IOException { l.release(); }
            public long position() { return l.position(); }
            public long size() { return l.size(); }
            public String toString() { return l.toString(); }
        }
    }

    public abstract static class Seekable {
        public abstract int read(byte[] buf, int offset, int length) throws IOException;
        public abstract int write(byte[] buf, int offset, int length) throws IOException;
        public abstract int length() throws IOException;
        public abstract void seek(int pos) throws IOException;
        public abstract void close() throws IOException;
        public abstract int pos() throws IOException;

        public void sync() throws IOException {
            throw new IOException("sync not implemented for " + getClass());
        }
        public void resize(long length) throws IOException {
            throw new IOException("resize not implemented for " + getClass());
        }
        /** If pos == 0 and size == 0 lock covers whole file. */
        public Lock lock(long pos, long size, boolean shared) throws IOException {
            throw new IOException("lock not implemented for " + getClass());
        }

        public int read() throws IOException {
            byte[] buf = new byte[1];
            int n = read(buf,0,1);
            return n == -1 ? -1 : buf[0]&0xff;
        }

        public int tryReadFully(byte[] buf, int off, int len) throws IOException {
            int total = 0;
            while(len > 0) {
                    int n = read(buf,off,len);
                    if(n == -1) break;
                    off += n;
                    len -= n;
                total += n;
            }
            return total == 0 ? -1 : total;
        }

        public static class ByteArray extends Seekable {
            protected byte[] data;
            protected int pos;
            private final boolean writable;

            public ByteArray(byte[] data, boolean writable) {
                this.data = data;
                this.pos = 0;
                this.writable = writable;
            }

            public int read(byte[] buf, int off, int len) {
                len = Math.min(len,data.length-pos);
                if(len <= 0) return -1;
                System.arraycopy(data,pos,buf,off,len);
                pos += len;
                return len;
            }

            public int write(byte[] buf, int off, int len) throws IOException {
                if(!writable) throw new IOException("read-only data");
                len = Math.min(len,data.length-pos);
                if(len <= 0) throw new IOException("no space");
                System.arraycopy(buf,off,data,pos,len);
                pos += len;
                return len;
            }

            public int length() { return data.length; }
            public int pos() { return pos; }
            public void seek(int pos) { this.pos = pos; }
            public void close() { /*noop*/ }
        }

        public static class File extends Seekable {
            private final java.io.File file;
            private final RandomAccessFile raf;

            public File(String fileName) throws IOException { this(fileName,false); }
            public File(String fileName, boolean writable) throws IOException { this(new java.io.File(fileName),writable,false); }

            public File(java.io.File file, boolean writable, boolean truncate) throws IOException {
                this.file = file;
                String mode = writable ? "rw" : "r";
                raf = new RandomAccessFile(file,mode);
                if (truncate) Platform.setFileLength(raf, 0);
            }

            public int read(byte[] buf, int offset, int length) throws IOException { return raf.read(buf,offset,length); }
            public int write(byte[] buf, int offset, int length) throws IOException { raf.write(buf,offset,length); return length; }
            public void sync() throws IOException { raf.getFD().sync(); }
            public void seek(int pos) throws IOException{ raf.seek(pos); }
            public int pos()  throws IOException { return (int) raf.getFilePointer(); }
            public int length() throws IOException { return (int)raf.length(); }
            public void close() throws IOException { raf.close(); }
            public void resize(long length) throws IOException { Platform.setFileLength(raf, (int)length); }
            public boolean equals(Object o) {
                return o != null && o instanceof File
                       && file.equals(((File)o).file);
            }
            public Lock lock(long pos, long size, boolean shared)
                    throws IOException {
                return Platform.lockFile(this, raf, pos, size, shared);
            }
        }

        public static class InputStream extends Seekable {
            private byte[] buffer = new byte[4096];
            private int bytesRead = 0;
            private boolean eof = false;
            private int pos;
            private java.io.InputStream is;

            public InputStream(java.io.InputStream is) { this.is = is; }

            public int read(byte[] outbuf, int off, int len) throws IOException {
                if(pos >= bytesRead && !eof) readTo(pos + 1);
                len = Math.min(len,bytesRead-pos);
                if(len <= 0) return -1;
                System.arraycopy(buffer,pos,outbuf,off,len);
                pos += len;
                return len;
            }

            private void readTo(int target) throws IOException {
                if(target >= buffer.length) {
                    byte[] buf2 = new byte[Math.max(buffer.length+Math.min(buffer.length,65536),target)];
                    System.arraycopy(buffer,0,buf2,0,bytesRead);
                    buffer = buf2;
                }
                while(bytesRead < target) {
                    int n = is.read(buffer,bytesRead,buffer.length-bytesRead);
                    if(n == -1) {
                        eof = true;
                        break;
                    }
                    bytesRead += n;
                }
            }

            public int length() throws IOException {
                while(!eof) readTo(bytesRead+4096);
                return bytesRead;
            }

            public int write(byte[] buf, int off, int len) throws IOException { throw new IOException("read-only"); }
            public void seek(int pos) { this.pos = pos; }
            public int pos() { return pos; }
            public void close() throws IOException { is.close(); }
        }

        public abstract static class Lock {
            private Object owner = null;

            public abstract Seekable seekable();
            public abstract boolean isShared();
            public abstract boolean isValid();
            public abstract void release() throws IOException;
            public abstract long position();
            public abstract long size();

            public void setOwner(Object o) { owner = o; }
            public Object getOwner() { return owner; }

            public final boolean contains(int start, int len) {
                return start >= position() &&  position() + size() >= start + len;
            }

            public final boolean contained(int start, int len) {
                return start < position() && position() + size() < start + len;
            }

            public final boolean overlaps(int start, int len) {
                return contains(start, len) || contained(start, len);
            }
        }
    }
    // Register Names
    public final static int ZERO = 0; // Immutable, hardwired to 0
    public final static int AT = 1;  // Reserved for assembler
    public final static int K0 = 26; // Reserved for kernel
    public final static int K1 = 27; // Reserved for kernel
    public final static int GP = 28; // Global pointer (the middle of .sdata/.sbss)
    public final static int SP = 29; // Stack pointer
    public final static int FP = 30; // Frame Pointer
    public final static int RA = 31; // Return Address

    // Return values (caller saved)
    public final static int V0 = 2;
    public final static int V1 = 3;
    // Argument Registers (caller saved)
    public final static int A0 = 4;
    public final static int A1 = 5;
    public final static int A2 = 6;
    public final static int A3 = 7;
    // Temporaries (caller saved)
    public final static int T0 = 8;
    public final static int T1 = 9;
    public final static int T2 = 10;
    public final static int T3 = 11;
    public final static int T4 = 12;
    public final static int T5 = 13;
    public final static int T6 = 14;
    public final static int T7 = 15;
    public final static int T8 = 24;
    public final static int T9 = 25;
    // Saved (callee saved)
    public final static int S0 = 16;
    public final static int S1 = 17;
    public final static int S2 = 18;
    public final static int S3 = 19;
    public final static int S4 = 20;
    public final static int S5 = 21;
    public final static int S6 = 22;
    public final static int S7 = 23;

    public static final int SYS_null = 0;
    public static final int SYS_exit = 1;
    public static final int SYS_pause = 2;
    public static final int SYS_open = 3;
    public static final int SYS_close = 4;
    public static final int SYS_read = 5;
    public static final int SYS_write = 6;
    public static final int SYS_sbrk = 7;
    public static final int SYS_fstat = 8;
    public static final int SYS_lseek = 10;
    public static final int SYS_kill = 11;
    public static final int SYS_getpid = 12;
    public static final int SYS_calljava = 13;
    public static final int SYS_stat = 14;
    public static final int SYS_gettimeofday = 15;
    public static final int SYS_sleep = 16;
    public static final int SYS_times = 17;
    public static final int SYS_mkdir = 18;
    public static final int SYS_getpagesize = 19;
    public static final int SYS_unlink = 20;
    public static final int SYS_utime = 21;
    public static final int SYS_chdir = 22;
    public static final int SYS_pipe = 23;
    public static final int SYS_dup2 = 24;
    public static final int SYS_fork = 25;
    public static final int SYS_waitpid = 26;
    public static final int SYS_getcwd = 27;
    public static final int SYS_exec = 28;
    public static final int SYS_fcntl = 29;
    public static final int SYS_rmdir = 30;
    public static final int SYS_sysconf = 31;
    public static final int SYS_readlink = 32;
    public static final int SYS_lstat = 33;
    public static final int SYS_symlink = 34;
    public static final int SYS_link = 35;
    public static final int SYS_getdents = 36;
    public static final int SYS_memcpy = 37;
    public static final int SYS_memset = 38;
    public static final int SYS_dup = 39;
    public static final int SYS_vfork = 40;
    public static final int SYS_chroot = 41;
    public static final int SYS_mknod = 42;
    public static final int SYS_lchown = 43;
    public static final int SYS_ftruncate = 44;
    public static final int SYS_usleep = 45;
    public static final int SYS_getppid = 46;
    public static final int SYS_mkfifo = 47;
    public static final int SYS_klogctl = 51;
    public static final int SYS_realpath = 52;
    public static final int SYS_sysctl = 53;
    public static final int SYS_setpriority = 54;
    public static final int SYS_getpriority = 55;
    public static final int SYS_socket = 56;
    public static final int SYS_connect = 57;
    public static final int SYS_resolve_hostname = 58;
    public static final int SYS_accept = 59;
    public static final int SYS_setsockopt = 60;
    public static final int SYS_getsockopt = 61;
    public static final int SYS_listen = 62;
    public static final int SYS_bind = 63;
    public static final int SYS_shutdown = 64;
    public static final int SYS_sendto = 65;
    public static final int SYS_recvfrom = 66;
    public static final int SYS_select = 67;
    public static final int SYS_getuid = 68;
    public static final int SYS_getgid = 69;
    public static final int SYS_geteuid = 70;
    public static final int SYS_getegid = 71;
    public static final int SYS_getgroups = 72;
    public static final int SYS_umask = 73;
    public static final int SYS_chmod = 74;
    public static final int SYS_fchmod = 75;
    public static final int SYS_chown = 76;
    public static final int SYS_fchown = 77;
    public static final int SYS_access = 78;
    public static final int SYS_alarm = 79;
    public static final int SYS_setuid = 80;
    public static final int SYS_setgid = 81;
    public static final int SYS_send = 82;
    public static final int SYS_recv = 83;
    public static final int SYS_getsockname = 84;
    public static final int SYS_getpeername = 85;
    public static final int SYS_seteuid = 86;
    public static final int SYS_setegid = 87;
    public static final int SYS_setgroups = 88;
    public static final int SYS_resolve_ip = 89;
    public static final int SYS_setsid = 90;
    public static final int SYS_fsync = 91;
    public static final int AF_UNIX = 1;
    public static final int AF_INET = 2;
    public static final int SOCK_STREAM = 1;
    public static final int SOCK_DGRAM = 2;
    public static final int HOST_NOT_FOUND = 1;
    public static final int TRY_AGAIN = 2;
    public static final int NO_RECOVERY = 3;
    public static final int NO_DATA = 4;
    public static final int SOL_SOCKET = 0xffff;
    public static final int SO_REUSEADDR = 0x0004;
    public static final int SO_KEEPALIVE = 0x0008;
    public static final int SO_BROADCAST = 0x0020;
    public static final int SO_TYPE = 0x1008;
    public static final int SHUT_RD = 0;
    public static final int SHUT_WR = 1;
    public static final int SHUT_RDWR = 2;
    public static final int INADDR_ANY = 0;
    public static final int INADDR_LOOPBACK = 0x7f000001;
    public static final int INADDR_BROADCAST = 0xffffffff;
    public static final int EPERM = 1; /* Not super-user */
    public static final int ENOENT = 2; /* No such file or directory */
    public static final int ESRCH = 3; /* No such process */
    public static final int EINTR = 4; /* Interrupted system call */
    public static final int EIO = 5; /* I/O error */
    public static final int ENXIO = 6; /* No such device or address */
    public static final int E2BIG = 7; /* Arg list too long */
    public static final int ENOEXEC = 8; /* Exec format error */
    public static final int EBADF = 9; /* Bad file number */
    public static final int ECHILD = 10; /* No children */
    public static final int EAGAIN = 11; /* No more processes */
    public static final int ENOMEM = 12; /* Not enough core */
    public static final int EACCES = 13; /* Permission denied */
    public static final int EFAULT = 14; /* Bad address */
    public static final int ENOTBLK = 15; /* Block device required */
    public static final int EBUSY = 16; /* Mount device busy */
    public static final int EEXIST = 17; /* File exists */
    public static final int EXDEV = 18; /* Cross-device link */
    public static final int ENODEV = 19; /* No such device */
    public static final int ENOTDIR = 20; /* Not a directory */
    public static final int EISDIR = 21; /* Is a directory */
    public static final int EINVAL = 22; /* Invalid argument */
    public static final int ENFILE = 23; /* Too many open files in system */
    public static final int EMFILE = 24; /* Too many open files */
    public static final int ENOTTY = 25; /* Not a typewriter */
    public static final int ETXTBSY = 26; /* Text file busy */
    public static final int EFBIG = 27; /* File too large */
    public static final int ENOSPC = 28; /* No space left on device */
    public static final int ESPIPE = 29; /* Illegal seek */
    public static final int EROFS = 30; /* Read only file system */
    public static final int EMLINK = 31; /* Too many links */
    public static final int EPIPE = 32; /* Broken pipe */
    public static final int EDOM = 33; /* Math arg out of domain of func */
    public static final int ERANGE = 34; /* Math result not representable */
    public static final int ENOMSG = 35; /* No message of desired type */
    public static final int EIDRM = 36; /* Identifier removed */
    public static final int ECHRNG = 37; /* Channel number out of range */
    public static final int EL2NSYNC = 38; /* Level 2 not synchronized */
    public static final int EL3HLT = 39; /* Level 3 halted */
    public static final int EL3RST = 40; /* Level 3 reset */
    public static final int ELNRNG = 41; /* Link number out of range */
    public static final int EUNATCH = 42; /* Protocol driver not attached */
    public static final int ENOCSI = 43; /* No CSI structure available */
    public static final int EL2HLT = 44; /* Level 2 halted */
    public static final int EDEADLK = 45; /* Deadlock condition */
    public static final int ENOLCK = 46; /* No record locks available */
    public static final int EBADE = 50; /* Invalid exchange */
    public static final int EBADR = 51; /* Invalid request descriptor */
    public static final int EXFULL = 52; /* Exchange full */
    public static final int ENOANO = 53; /* No anode */
    public static final int EBADRQC = 54; /* Invalid request code */
    public static final int EBADSLT = 55; /* Invalid slot */
    public static final int EDEADLOCK = 56; /* File locking deadlock error */
    public static final int EBFONT = 57; /* Bad font file fmt */
    public static final int ENOSTR = 60; /* Device not a stream */
    public static final int ENODATA = 61; /* No data (for no delay io) */
    public static final int ETIME = 62; /* Timer expired */
    public static final int ENOSR = 63; /* Out of streams resources */
    public static final int ENONET = 64; /* Machine is not on the network */
    public static final int ENOPKG = 65; /* Package not installed */
    public static final int EREMOTE = 66; /* The object is remote */
    public static final int ENOLINK = 67; /* The link has been severed */
    public static final int EADV = 68; /* Advertise error */
    public static final int ESRMNT = 69; /* Srmount error */
    public static final int ECOMM = 70; /* Communication error on send */
    public static final int EPROTO = 71; /* Protocol error */
    public static final int EMULTIHOP = 74; /* Multihop attempted */
    public static final int ELBIN = 75; /* Inode is remote (not really error) */
    public static final int EDOTDOT = 76; /* Cross mount point (not really error) */
    public static final int EBADMSG = 77; /* Trying to read unreadable message */
    public static final int EFTYPE = 79; /* Inappropriate file type or format */
    public static final int ENOTUNIQ = 80; /* Given log. name not unique */
    public static final int EBADFD = 81; /* f.d. invalid for this operation */
    public static final int EREMCHG = 82; /* Remote address changed */
    public static final int ELIBACC = 83; /* Can't access a needed shared lib */
    public static final int ELIBBAD = 84; /* Accessing a corrupted shared lib */
    public static final int ELIBSCN = 85; /* .lib section in a.out corrupted */
    public static final int ELIBMAX = 86; /* Attempting to link in too many libs */
    public static final int ELIBEXEC = 87; /* Attempting to exec a shared library */
    public static final int ENOSYS = 88; /* Function not implemented */
    public static final int ENMFILE = 89; /* No more files */
    public static final int ENOTEMPTY = 90; /* Directory not empty */
    public static final int ENAMETOOLONG = 91; /* File or path name too long */
    public static final int ELOOP = 92; /* Too many symbolic links */
    public static final int EOPNOTSUPP = 95; /* Operation not supported on transport endpoint */
    public static final int EPFNOSUPPORT = 96; /* Protocol family not supported */
    public static final int ECONNRESET = 104; /* Connection reset by peer */
    public static final int ENOBUFS = 105; /* No buffer space available */
    public static final int EAFNOSUPPORT = 106; /* Address family not supported by protocol family */
    public static final int EPROTOTYPE = 107; /* Protocol wrong type for socket */
    public static final int ENOTSOCK = 108; /* Socket operation on non-socket */
    public static final int ENOPROTOOPT = 109; /* Protocol not available */
    public static final int ESHUTDOWN = 110; /* Can't send after socket shutdown */
    public static final int ECONNREFUSED = 111; /* Connection refused */
    public static final int EADDRINUSE = 112; /* Address already in use */
    public static final int ECONNABORTED = 113; /* Connection aborted */
    public static final int ENETUNREACH = 114; /* Network is unreachable */
    public static final int ENETDOWN = 115; /* Network interface is not configured */
    public static final int ETIMEDOUT = 116; /* Connection timed out */
    public static final int EHOSTDOWN = 117; /* Host is down */
    public static final int EHOSTUNREACH = 118; /* Host is unreachable */
    public static final int EINPROGRESS = 119; /* Connection already in progress */
    public static final int EALREADY = 120; /* Socket already connected */
    public static final int EDESTADDRREQ = 121; /* Destination address required */
    public static final int EMSGSIZE = 122; /* Message too long */
    public static final int EPROTONOSUPPORT = 123; /* Unknown protocol */
    public static final int ESOCKTNOSUPPORT = 124; /* Socket type not supported */
    public static final int EADDRNOTAVAIL = 125; /* Address not available */
    public static final int ENETRESET = 126;
    public static final int EISCONN = 127; /* Socket is already connected */
    public static final int ENOTCONN = 128; /* Socket is not connected */
    public static final int ETOOMANYREFS = 129;
    public static final int EPROCLIM = 130;
    public static final int EUSERS = 131;
    public static final int EDQUOT = 132;
    public static final int ESTALE = 133;
    public static final int ENOTSUP = 134; /* Not supported */
    public static final int ENOMEDIUM = 135; /* No medium (in tape drive) */
    public static final int ENOSHARE = 136; /* No such host or network path */
    public static final int ECASECLASH = 137; /* Filename exists with different case */
    public static final int EILSEQ = 138;
    public static final int EOVERFLOW = 139; /* Value too large for defined data type */
    public static final int __ELASTERROR = 2000; /* Users can add values starting here */
    public static final int F_OK = 0;
    public static final int R_OK = 4;
    public static final int W_OK = 2;
    public static final int X_OK = 1;
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    public static final int STDIN_FILENO = 0; /* standard input file descriptor */
    public static final int STDOUT_FILENO = 1; /* standard output file descriptor */
    public static final int STDERR_FILENO = 2; /* standard error file descriptor */
    public static final int _SC_ARG_MAX = 0;
    public static final int _SC_CHILD_MAX = 1;
    public static final int _SC_CLK_TCK = 2;
    public static final int _SC_NGROUPS_MAX = 3;
    public static final int _SC_OPEN_MAX = 4;
    public static final int _SC_JOB_CONTROL = 5;
    public static final int _SC_SAVED_IDS = 6;
    public static final int _SC_VERSION = 7;
    public static final int _SC_PAGESIZE = 8;
    public static final int _SC_NPROCESSORS_CONF = 9;
    public static final int _SC_NPROCESSORS_ONLN = 10;
    public static final int _SC_PHYS_PAGES = 11;
    public static final int _SC_AVPHYS_PAGES = 12;
    public static final int _SC_MQ_OPEN_MAX = 13;
    public static final int _SC_MQ_PRIO_MAX = 14;
    public static final int _SC_RTSIG_MAX = 15;
    public static final int _SC_SEM_NSEMS_MAX = 16;
    public static final int _SC_SEM_VALUE_MAX = 17;
    public static final int _SC_SIGQUEUE_MAX = 18;
    public static final int _SC_TIMER_MAX = 19;
    public static final int _SC_TZNAME_MAX = 20;
    public static final int _SC_ASYNCHRONOUS_IO = 21;
    public static final int _SC_FSYNC = 22;
    public static final int _SC_MAPPED_FILES = 23;
    public static final int _SC_MEMLOCK = 24;
    public static final int _SC_MEMLOCK_RANGE = 25;
    public static final int _SC_MEMORY_PROTECTION = 26;
    public static final int _SC_MESSAGE_PASSING = 27;
    public static final int _SC_PRIORITIZED_IO = 28;
    public static final int _SC_REALTIME_SIGNALS = 29;
    public static final int _SC_SEMAPHORES = 30;
    public static final int _SC_SHARED_MEMORY_OBJECTS = 31;
    public static final int _SC_SYNCHRONIZED_IO = 32;
    public static final int _SC_TIMERS = 33;
    public static final int _SC_AIO_LISTIO_MAX = 34;
    public static final int _SC_AIO_MAX = 35;
    public static final int _SC_AIO_PRIO_DELTA_MAX = 36;
    public static final int _SC_DELAYTIMER_MAX = 37;
    public static final int _SC_THREAD_KEYS_MAX = 38;
    public static final int _SC_THREAD_STACK_MIN = 39;
    public static final int _SC_THREAD_THREADS_MAX = 40;
    public static final int _SC_TTY_NAME_MAX = 41;
    public static final int _SC_THREADS = 42;
    public static final int _SC_THREAD_ATTR_STACKADDR = 43;
    public static final int _SC_THREAD_ATTR_STACKSIZE = 44;
    public static final int _SC_THREAD_PRIORITY_SCHEDULING = 45;
    public static final int _SC_THREAD_PRIO_INHERIT = 46;
    public static final int _SC_THREAD_PRIO_PROTECT = 47;
    public static final int _SC_THREAD_PROCESS_SHARED = 48;
    public static final int _SC_THREAD_SAFE_FUNCTIONS = 49;
    public static final int _SC_GETGR_R_SIZE_MAX = 50;
    public static final int _SC_GETPW_R_SIZE_MAX = 51;
    public static final int _SC_LOGIN_NAME_MAX = 52;
    public static final int _SC_THREAD_DESTRUCTOR_ITERATIONS = 53;
    public static final int _SC_STREAM_MAX = 100;
    public static final int _SC_PRIORITY_SCHEDULING = 101;
    public static final int _PC_LINK_MAX = 0;
    public static final int _PC_MAX_CANON = 1;
    public static final int _PC_MAX_INPUT = 2;
    public static final int _PC_NAME_MAX = 3;
    public static final int _PC_PATH_MAX = 4;
    public static final int _PC_PIPE_BUF = 5;
    public static final int _PC_CHOWN_RESTRICTED = 6;
    public static final int _PC_NO_TRUNC = 7;
    public static final int _PC_VDISABLE = 8;
    public static final int _PC_ASYNC_IO = 9;
    public static final int _PC_PRIO_IO = 10;
    public static final int _PC_SYNC_IO = 11;
    public static final int _PC_POSIX_PERMISSIONS = 90;
    public static final int _PC_POSIX_SECURITY = 91;
    public static final int MAXPATHLEN = 1024;
    public static final int ARG_MAX = 65536; /* max bytes for an exec function */
    public static final int CHILD_MAX = 40; /* max simultaneous processes */
    public static final int LINK_MAX = 32767; /* max file link count */
    public static final int MAX_CANON = 255; /* max bytes in term canon input line */
    public static final int MAX_INPUT = 255; /* max bytes in terminal input */
    public static final int NAME_MAX = 255; /* max bytes in a file name */
    public static final int NGROUPS_MAX = 16; /* max supplemental group id's */
    public static final int OPEN_MAX = 64; /* max open files per process */
    public static final int PATH_MAX = 1024; /* max bytes in pathname */
    public static final int PIPE_BUF = 512; /* max bytes for atomic pipe writes */
    public static final int IOV_MAX = 1024; /* max elements in i/o vector */
    public static final int BC_BASE_MAX = 99; /* max ibase/obase values in bc(1) */
    public static final int BC_DIM_MAX = 2048; /* max array elements in bc(1) */
    public static final int BC_SCALE_MAX = 99; /* max scale value in bc(1) */
    public static final int BC_STRING_MAX = 1000; /* max const string length in bc(1) */
    public static final int COLL_WEIGHTS_MAX = 0; /* max weights for order keyword */
    public static final int EXPR_NEST_MAX = 32; /* max expressions nested in expr(1) */
    public static final int LINE_MAX = 2048; /* max bytes in an input line */
    public static final int RE_DUP_MAX = 255; /* max RE's in interval notation */
    public static final int CTL_MAXNAME = 12;
    public static final int CTL_UNSPEC = 0; /* unused */
    public static final int CTL_KERN = 1; /* "high kernel": proc, limits */
    public static final int CTL_VM = 2; /* virtual memory */
    public static final int CTL_VFS = 3; /* file system, mount type is next */
    public static final int CTL_NET = 4; /* network, see socket.h */
    public static final int CTL_DEBUG = 5; /* debugging parameters */
    public static final int CTL_HW = 6; /* generic cpu/io */
    public static final int CTL_MACHDEP = 7; /* machine dependent */
    public static final int CTL_USER = 8; /* user-level */
    public static final int CTL_P1003_1B = 9; /* POSIX 1003.1B */
    public static final int CTL_MAXID = 10; /* number of valid top-level ids */
    public static final int KERN_OSTYPE = 1; /* string: system version */
    public static final int KERN_OSRELEASE = 2; /* string: system release */
    public static final int KERN_OSREV = 3; /* int: system revision */
    public static final int KERN_VERSION = 4; /* string: compile time info */
    public static final int KERN_MAXVNODES = 5; /* int: max vnodes */
    public static final int KERN_MAXPROC = 6; /* int: max processes */
    public static final int KERN_MAXFILES = 7; /* int: max open files */
    public static final int KERN_ARGMAX = 8; /* int: max arguments to exec */
    public static final int KERN_SECURELVL = 9; /* int: system security level */
    public static final int KERN_HOSTNAME = 10; /* string: hostname */
    public static final int KERN_HOSTID = 11; /* int: host identifier */
    public static final int KERN_CLOCKRATE = 12; /* struct: struct clockrate */
    public static final int KERN_VNODE = 13; /* struct: vnode structures */
    public static final int KERN_PROC = 14; /* struct: process entries */
    public static final int KERN_FILE = 15; /* struct: file entries */
    public static final int KERN_PROF = 16; /* node: kernel profiling info */
    public static final int KERN_POSIX1 = 17; /* int: POSIX.1 version */
    public static final int KERN_NGROUPS = 18; /* int: # of supplemental group ids */
    public static final int KERN_JOB_CONTROL = 19; /* int: is job control available */
    public static final int KERN_SAVED_IDS = 20; /* int: saved set-user/group-ID */
    public static final int KERN_BOOTTIME = 21; /* struct: time kernel was booted */
    public static final int KERN_NISDOMAINNAME = 22; /* string: YP domain name */
    public static final int KERN_UPDATEINTERVAL = 23; /* int: update process sleep time */
    public static final int KERN_OSRELDATE = 24; /* int: OS release date */
    public static final int KERN_NTP_PLL = 25; /* node: NTP PLL control */
    public static final int KERN_BOOTFILE = 26; /* string: name of booted kernel */
    public static final int KERN_MAXFILESPERPROC = 27; /* int: max open files per proc */
    public static final int KERN_MAXPROCPERUID = 28; /* int: max processes per uid */
    public static final int KERN_DUMPDEV = 29; /* dev_t: device to dump on */
    public static final int KERN_IPC = 30; /* node: anything related to IPC */
    public static final int KERN_DUMMY = 31; /* unused */
    public static final int KERN_PS_STRINGS = 32; /* int: address of PS_STRINGS */
    public static final int KERN_USRSTACK = 33; /* int: address of USRSTACK */
    public static final int KERN_LOGSIGEXIT = 34; /* int: do we log sigexit procs? */
    public static final int KERN_MAXID = 35; /* number of valid kern ids */
    public static final int KERN_PROC_ALL = 0; /* everything */
    public static final int KERN_PROC_PID = 1; /* by process id */
    public static final int KERN_PROC_PGRP = 2; /* by process group id */
    public static final int KERN_PROC_SESSION = 3; /* by session of pid */
    public static final int KERN_PROC_TTY = 4; /* by controlling tty */
    public static final int KERN_PROC_UID = 5; /* by effective uid */
    public static final int KERN_PROC_RUID = 6; /* by real uid */
    public static final int KERN_PROC_ARGS = 7; /* get/set arguments/proctitle */
    public static final int KIPC_MAXSOCKBUF = 1; /* int: max size of a socket buffer */
    public static final int KIPC_SOCKBUF_WASTE = 2; /* int: wastage factor in sockbuf */
    public static final int KIPC_SOMAXCONN = 3; /* int: max length of connection q */
    public static final int KIPC_MAX_LINKHDR = 4; /* int: max length of link header */
    public static final int KIPC_MAX_PROTOHDR = 5; /* int: max length of network header */
    public static final int KIPC_MAX_HDR = 6; /* int: max total length of headers */
    public static final int KIPC_MAX_DATALEN = 7; /* int: max length of data? */
    public static final int KIPC_MBSTAT = 8; /* struct: mbuf usage statistics */
    public static final int KIPC_NMBCLUSTERS = 9; /* int: maximum mbuf clusters */
    public static final int HW_MACHINE = 1; /* string: machine class */
    public static final int HW_MODEL = 2; /* string: specific machine model */
    public static final int HW_NCPU = 3; /* int: number of cpus */
    public static final int HW_BYTEORDER = 4; /* int: machine byte order */
    public static final int HW_PHYSMEM = 5; /* int: total memory */
    public static final int HW_USERMEM = 6; /* int: non-kernel memory */
    public static final int HW_PAGESIZE = 7; /* int: software page size */
    public static final int HW_DISKNAMES = 8; /* strings: disk drive names */
    public static final int HW_DISKSTATS = 9; /* struct: diskstats[] */
    public static final int HW_FLOATINGPT = 10; /* int: has HW floating point? */
    public static final int HW_MACHINE_ARCH = 11; /* string: machine architecture */
    public static final int HW_MAXID = 12; /* number of valid hw ids */
    public static final int USER_CS_PATH = 1; /* string: _CS_PATH */
    public static final int USER_BC_BASE_MAX = 2; /* int: BC_BASE_MAX */
    public static final int USER_BC_DIM_MAX = 3; /* int: BC_DIM_MAX */
    public static final int USER_BC_SCALE_MAX = 4; /* int: BC_SCALE_MAX */
    public static final int USER_BC_STRING_MAX = 5; /* int: BC_STRING_MAX */
    public static final int USER_COLL_WEIGHTS_MAX = 6; /* int: COLL_WEIGHTS_MAX */
    public static final int USER_EXPR_NEST_MAX = 7; /* int: EXPR_NEST_MAX */
    public static final int USER_LINE_MAX = 8; /* int: LINE_MAX */
    public static final int USER_RE_DUP_MAX = 9; /* int: RE_DUP_MAX */
    public static final int USER_POSIX2_VERSION = 10; /* int: POSIX2_VERSION */
    public static final int USER_POSIX2_C_BIND = 11; /* int: POSIX2_C_BIND */
    public static final int USER_POSIX2_C_DEV = 12; /* int: POSIX2_C_DEV */
    public static final int USER_POSIX2_CHAR_TERM = 13; /* int: POSIX2_CHAR_TERM */
    public static final int USER_POSIX2_FORT_DEV = 14; /* int: POSIX2_FORT_DEV */
    public static final int USER_POSIX2_FORT_RUN = 15; /* int: POSIX2_FORT_RUN */
    public static final int USER_POSIX2_LOCALEDEF = 16; /* int: POSIX2_LOCALEDEF */
    public static final int USER_POSIX2_SW_DEV = 17; /* int: POSIX2_SW_DEV */
    public static final int USER_POSIX2_UPE = 18; /* int: POSIX2_UPE */
    public static final int USER_STREAM_MAX = 19; /* int: POSIX2_STREAM_MAX */
    public static final int USER_TZNAME_MAX = 20; /* int: POSIX2_TZNAME_MAX */
    public static final int USER_MAXID = 21; /* number of valid user ids */
    public static final int CTL_P1003_1B_ASYNCHRONOUS_IO = 1; /* boolean */
    public static final int CTL_P1003_1B_MAPPED_FILES = 2; /* boolean */
    public static final int CTL_P1003_1B_MEMLOCK = 3; /* boolean */
    public static final int CTL_P1003_1B_MEMLOCK_RANGE = 4; /* boolean */
    public static final int CTL_P1003_1B_MEMORY_PROTECTION = 5; /* boolean */
    public static final int CTL_P1003_1B_MESSAGE_PASSING = 6; /* boolean */
    public static final int CTL_P1003_1B_PRIORITIZED_IO = 7; /* boolean */
    public static final int CTL_P1003_1B_PRIORITY_SCHEDULING = 8; /* boolean */
    public static final int CTL_P1003_1B_REALTIME_SIGNALS = 9; /* boolean */
    public static final int CTL_P1003_1B_SEMAPHORES = 10; /* boolean */
    public static final int CTL_P1003_1B_FSYNC = 11; /* boolean */
    public static final int CTL_P1003_1B_SHARED_MEMORY_OBJECTS = 12; /* boolean */
    public static final int CTL_P1003_1B_SYNCHRONIZED_IO = 13; /* boolean */
    public static final int CTL_P1003_1B_TIMERS = 14; /* boolean */
    public static final int CTL_P1003_1B_AIO_LISTIO_MAX = 15; /* int */
    public static final int CTL_P1003_1B_AIO_MAX = 16; /* int */
    public static final int CTL_P1003_1B_AIO_PRIO_DELTA_MAX = 17; /* int */
    public static final int CTL_P1003_1B_DELAYTIMER_MAX = 18; /* int */
    public static final int CTL_P1003_1B_MQ_OPEN_MAX = 19; /* int */
    public static final int CTL_P1003_1B_PAGESIZE = 20; /* int */
    public static final int CTL_P1003_1B_RTSIG_MAX = 21; /* int */
    public static final int CTL_P1003_1B_SEM_NSEMS_MAX = 22; /* int */
    public static final int CTL_P1003_1B_SEM_VALUE_MAX = 23; /* int */
    public static final int CTL_P1003_1B_SIGQUEUE_MAX = 24; /* int */
    public static final int CTL_P1003_1B_TIMER_MAX = 25; /* int */
    public static final int CTL_P1003_1B_MAXID = 26;
    public static final int F_UNLKSYS = 4;
    public static final int F_CNVT = 12;
    public static final int F_SETFD = 2;
    public static final int F_SETFL = 4;
    public static final int F_SETLK = 8;
    public static final int F_SETOWN = 6;
    public static final int F_RDLCK = 1;
    public static final int F_WRLCK = 2;
    public static final int F_SETLKW = 9;
    public static final int F_GETFD = 1;
    public static final int F_DUPFD = 0;
    public static final int O_WRONLY = 1;
    public static final int F_RSETLKW = 13;
    public static final int O_RDWR = 2;
    public static final int F_RGETLK = 10;
    public static final int O_RDONLY = 0;
    public static final int F_UNLCK = 3;
    public static final int F_GETOWN = 5;
    public static final int F_RSETLK = 11;
    public static final int F_GETFL = 3;
    public static final int F_GETLK = 7;

}
