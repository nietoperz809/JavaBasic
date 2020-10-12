package midisystem;


import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.ShortMessage;

/**
 *
 * @author Administrator
 */
class EventMaker
{
    /*	This velocity is used for all notes.
     */
    private static final int VELOCITY = 127;
    static private MidiChannel[] channels;
    private final int channel;  // Used channel
    
    /**
     *
     * @param ch
     */
    public EventMaker(int ch)
    {
        channel = ch;
        try
        {
            channels =  MidiSystem.getSynthesizer().getChannels();
        }
        catch (MidiUnavailableException ignored)
        {
            System.err.println ("MODI not available");
        }
    }
    
    /**
     *
     * @param note
     * @param time
     * @return
     */
    public MidiEvent on(int note, int time)
    {
        ShortMessage a;
        try
        {
            a = new ShortMessage(ShortMessage.NOTE_ON, channel, note, VELOCITY);
        }
        catch (InvalidMidiDataException ex)
        {
            return null;
        }
        return new MidiEvent(a, time);
    }

    /**
     *
     * @param note
     * @param time
     * @return
     */
    public MidiEvent off(int note, int time)
    {
        ShortMessage b;
        try
        {
            b = new ShortMessage(ShortMessage.NOTE_OFF, channel, note, VELOCITY);
        }
        catch (InvalidMidiDataException ex)
        {
            return null;
        }
        return new MidiEvent(b, time);
    }

    /**
     *
     * @param pause
     * @return
     */
    public MidiEvent insertPause(int pause)
    {
        ShortMessage a;
        try
        {
            a = new ShortMessage(ShortMessage.TIMING_CLOCK);
        }
        catch (InvalidMidiDataException ex)
        {
            return null;
        }
        return new MidiEvent(a, pause);
    }

    /**
     *
     * @param pause
     * @return
     */
    public MidiEvent insertStop(int pause)
    {
        ShortMessage a;
        try
        {
            a = new ShortMessage(ShortMessage.STOP);
        }
        catch (InvalidMidiDataException ex)
        {
            return null;
        }
        return new MidiEvent(a, pause);
    }
    
    /**
     *
     * @param instr
     * @param time
     * @return
     */
    public MidiEvent changeInstrument(Instrument instr, int time)
    {
        Patch pa = instr.getPatch();
        channels[channel].programChange(pa.getBank(), pa.getProgram());
        ShortMessage in;
        try
        {
            in = new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, pa.getProgram(), pa.getBank());
        }
        catch (InvalidMidiDataException ex)
        {
            return null;
        }
        return new MidiEvent(in, time);
    }
}

