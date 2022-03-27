package midisystem;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

/**
 * @author Administrator
 */
public class MidiSynthSystem {
    private static MidiSynthSystem this_mss;

    public static boolean wasUsed() {
        return this_mss != null;
    }

    /**
     * Get the singleton instance
     *
     * @return
     */
    public static MidiSynthSystem get() {
        if (this_mss == null) {
            try {
                this_mss = new MidiSynthSystem();
                this_mss.deleteAllTracks();
            } catch (Exception ex) {
                return null;
            }
        }
        return this_mss;
    }

    private final Sequencer sm_sequencer;
    private final Synthesizer sm_synthesizer;
    private Sequence sm_sequence;
    private int mult = 1;
    private final Instrument[] orchestra;
    private final Object waitObject;
    private boolean doWait = false;

    public void setSpeed(int s) throws InvalidMidiDataException {
        mult = s;
        sm_sequence = new Sequence(Sequence.SMPTE_30, mult);
    }

    /**
     * Private Constructor
     *
     * @throws Exception If smth goes wrong
     */
    private MidiSynthSystem() throws Exception {
        sm_sequencer = MidiSystem.getSequencer();
        sm_sequencer.close();
        sm_synthesizer = MidiSystem.getSynthesizer();
        sm_synthesizer.close();
        sm_sequence = new Sequence(Sequence.SMPTE_30, mult);

        sm_synthesizer.open();
        sm_sequencer.open();
        sm_sequencer.stop();
        orchestra = sm_synthesizer.getAvailableInstruments();
        waitObject = new Object();
    }

    /**
     * Shuts down the sound system
     */
    public void shutdown() {
        if (this_mss == null) {
            return;
        }
        sm_sequencer.stop();
        sm_sequencer.close();
        sm_synthesizer.close();
        this_mss = null;
    }

    /**
     * Get Sequencer
     *
     * @return
     */
    public Sequencer getSequencer() {
        return sm_sequencer;
    }

// --Commented out by Inspection START (10/19/2020 1:56 AM):
//    /**
//     * Get Synthesizer
//     *
//     * @return
//     */
//    public Synthesizer getSynthesizer()
//    {
//        return sm_synthesizer;
//    }
// --Commented out by Inspection STOP (10/19/2020 1:56 AM)

    /**
     * Get Sequence
     *
     * @return
     */
    public Sequence getSequence() {
        return sm_sequence;
    }

    /**
     * Get one Instrument from instrument bank
     *
     * @param idx Instr index
     * @return
     */
    public Instrument getInstrument(int idx) {
        if (idx < 0 || idx >= orchestra.length) {
            return null;
        }
        return orchestra[idx];
    }

    public Instrument[] getInstruments() {
        return orchestra;
    }

    /**
     * Set the number of Loops
     *
     * @param i
     */
    public void setLoops(int i) {
        sm_sequencer.setLoopCount(i);
    }

    /**
     * Starts playing the sequence
     *
     * @return
     */
    public boolean start() {
        try {
            sm_sequencer.setSequence(sm_sequence);
        } catch (InvalidMidiDataException ex) {
            return false;
        }
        sm_sequencer.addMetaEventListener((MetaMessage event) ->
        {
            if (event.getType() == 47) {
                if (!doWait) {
                    System.out.println("spurious");
                    return;
                }
                System.out.println("end of midi " + System.currentTimeMillis());
                try {
                    //Thread.sleep(200);
                    deleteAllTracks();
                    synchronized (waitObject) {
                        doWait = false;
                        waitObject.notify();
                    }
                } catch (Exception ignored) {
                }
            }
        });
        sm_sequencer.start();
        return true;
    }

    /**
     * Wait for end of playing
     *
     * @throws InterruptedException
     */
    public void waitUntilEnd() throws InterruptedException {
        System.out.println("wait for endmidi " + System.currentTimeMillis());
        synchronized (waitObject) {
            doWait = true;
            waitObject.wait();
        }
    }

    /**
     * Deletes all tracks
     */
    public void deleteAllTracks() throws Exception {
        sm_sequence = new Sequence(Sequence.SMPTE_30, mult);
    }
}
