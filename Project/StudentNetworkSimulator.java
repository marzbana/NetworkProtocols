import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     * int MAXDATASIZE : the maximum size of the Message data and
     * Packet payload
     *
     * int A : a predefined integer that represents entity A
     * int B : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     * void stopTimer(int entity):
     * Stops the timer running at "entity" [A or B]
     * void startTimer(int entity, double increment):
     * Starts a timer running at "entity" [A or B], which will expire in
     * "increment" time units, causing the interrupt handler to be
     * called. You should only call this with A.
     * void toLayer3(int callingEntity, Packet p)
     * Puts the packet "p" into the network from "callingEntity" [A or B]
     * void toLayer5(String dataSent)
     * Passes "dataSent" up to layer 5
     * double getTime()
     * Returns the current time in the simulator. Might be useful for
     * debugging.
     * int getTraceLevel()
     * Returns TraceLevel
     * void printEventList()
     * Prints the current event list to stdout. Might be useful for
     * debugging, but probably not.
     *
     *
     * Predefined Classes:
     *
     * Message: Used to encapsulate a message coming from layer 5
     * Constructor:
     * Message(String inputData):
     * creates a new Message containing "inputData"
     * Methods:
     * boolean setData(String inputData):
     * sets an existing Message's data to "inputData"
     * returns true on success, false otherwise
     * String getData():
     * returns the data contained in the message
     * Packet: Used to encapsulate a packet
     * Constructors:
     * Packet (Packet p):
     * creates a new Packet that is a copy of "p"
     * Packet (int seq, int ack, int check, String newPayload)
     * creates a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and a
     * payload of "newPayload"
     * Packet (int seq, int ack, int check)
     * chreate a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and
     * an empty payload
     * Methods:
     * boolean setSeqnum(int n)
     * sets the Packet's sequence field to "n"
     * returns true on success, false otherwise
     * boolean setAcknum(int n)
     * sets the Packet's ack field to "n"
     * returns true on success, false otherwise
     * boolean setChecksum(int n)
     * sets the Packet's checksum to "n"
     * returns true on success, false otherwise
     * boolean setPayload(String newPayload)
     * sets the Packet's payload to "newPayload"
     * returns true on success, false otherwise
     * int getSeqnum()
     * returns the contents of the Packet's sequence field
     * int getAcknum()
     * returns the contents of the Packet's ack field
     * int getChecksum()
     * returns the checksum of the Packet
     * int getPayload()
     * returns the Packet's payload
     *
     */

    /*
     * Please use the following variables in your routines.
     * int WindowSize : the window size
     * double RxmtInterval : the retransmission timeout
     * int LimitSeqNo : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private static int WindowSize;
    private double RxmtInterval;
    private static int LimitSeqNo;

    // Add any necessary class variables here. Remember, you cannot use
    // these variables to send messages error free! They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    // variables to use in program
    private static int base;
    private static int prev;

    private static int nextSeqNum;
    // window
    private static Packet window[];
    // buffer
    private static Packet buffer[];
    // next sequ number
    private static int expectedSeqNum;
    private static int size;
    private static int bufferIndex;
    private static int maxWin;
    private static Packet lastB;
    // variables for calulations and stats at the end
    private static int original_packets;
    private static int retransmitted_packets;
    private static int messageNumber;
    // b
    private static int toLayer5Count;
    private static int sentActs;
    private static int corruptedPackets;
    private static double[][] time;

    // calculating rtt at stats at end
    protected static double calcRTT() {
        double rtt = 0;

        int count = 0;
        for (int i = 0; i < messageNumber; i++) {
            if (time[i][1] == 0 && time[i][2] != 0) {
                rtt += time[i][2] - time[i][0];
                count++;
            }
        }
        return rtt / count;

    }

    // calculating throughput at stats at end
    protected static double calcComT() {
        double comT = 0;
        int count = 0;
        for (int i = 0; i < messageNumber; i++) {
            if (time[i][2] != 0) {
                comT += time[i][2] - time[i][0];
                count++;
            }
        }
        return comT / count;
    }

    // updtes send time
    protected static void updateSentTime(int msg, double tm) {
        if (time[msg][0] == 0) {
            time[msg][0] = tm;
        } else {
            time[msg][1] = tm;
        }
    }

    // updates received time
    protected static void updateReceivedTime(int msg, double tm) {
        time[msg][2] = tm;
    }

    // adjusts nextSeqNum
    protected static void adjustNext() {
        if (nextSeqNum == (LimitSeqNo - 1)) {
            nextSeqNum = 0;
        } else {
            nextSeqNum++;
        }
    }

    // adjusts base
    protected static void adjustBase() {
        if (base == (LimitSeqNo - 1)) {
            base = 0;
        } else {
            base++;
        }
    }

    // if window is full
    protected static boolean windowFull() {
        if (nextSeqNum >= base) {
            if ((nextSeqNum - base) > size) {
                return false;
            } else {
                return true;
            }

        } else {
            if (((LimitSeqNo - base) + nextSeqNum) > size) {
                return false;
            } else {
                return true;
            }
        }
    }

    // This is the constructor. Don't touch!
    public StudentNetworkSimulator(int numMessages,
            double loss,
            double corrupt,
            double avgDelay,
            int trace,
            int seed,
            int winsize,
            double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize * 2; // set appropriately
        RxmtInterval = delay;
        // variables used for stats
        corruptedPackets = 0;
        time = new double[numMessages][3];
        original_packets = 0;
        retransmitted_packets = 0;
        toLayer5Count = 0;

    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send. It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        // checks if the window is full
        if (windowFull()) {
            original_packets++;
            // adds packet to window
            window[nextSeqNum] = new Packet(nextSeqNum, messageNumber, makeSenderChecksum(nextSeqNum,
                    messageNumber, message.getData()), message.getData());
            // sends packet
            toLayer3(0,
                    window[nextSeqNum]);

            updateSentTime(messageNumber, getTime());
            messageNumber++;
            if (traceLevel == 1) {
                System.out.println("Time: " + getTime() + " A: Packet " + window[nextSeqNum] + " sent");
            }
            if (base == nextSeqNum) {
                startTimer(0, RxmtInterval);
            }
            adjustNext();

        } else {
            if (bufferIndex < LimitSeqNo) {

                buffer[++bufferIndex] = new Packet(0, 0, 0, message.getData());

            }

            if (traceLevel == 1) {
                System.out.println(
                        "Time: " + getTime() + " A: message " + buffer[bufferIndex].getPayload() + " buffered");
            }
        }

    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        // checks checksum

        if (checkChecksum_receiver(packet)) {
            // checks if the base is wrapped around or not and if the correct ack is
            // received
            if ((packet.getAcknum() >= base)) {
                System.out.println("A received succesful ack: " + packet.getAcknum());
                int i = base;
                int c = packet.getAcknum();
                // stops timer
                stopTimer(0);
                for (; i <= c; i++) {
                    if (traceLevel == 1) {
                        System.out
                                .println("Time: " + getTime() + " A: Packet " + window[i]
                                        + " recevied succesful ack");
                    }
                    updateReceivedTime(window[i].getAcknum(), getTime());
                    adjustBase();

                }
                // starts the timer if neccesrary
                if (base != nextSeqNum) {
                    startTimer(0, RxmtInterval);
                }
                if (bufferIndex >= 0) {
                    if (traceLevel == 1) {
                        System.out.println("Time: " + getTime() + " A: buffered message " + buffer[bufferIndex]
                                + " sent_to_aOutput");
                    }
                    Message x = new Message(buffer[bufferIndex--].getPayload());
                    aOutput(x);

                }

            } else if (packet.getAcknum() < base && base > nextSeqNum) {
                System.out.println("A received succesful ack: " + packet.getAcknum());
                int i = base;
                stopTimer(0);
                if (packet.getAcknum() < base) {
                    for (; i < LimitSeqNo; i++) {
                        if (traceLevel == 1) {
                            System.out.println(
                                    "Time: " + getTime() + " A: Packet " + window[i] + " recevied succesful ack");
                        }
                        updateReceivedTime(window[i].getAcknum(), getTime());
                        adjustBase();
                    }
                    for (i = 0; i <= packet.getAcknum(); i++) {
                        if (traceLevel == 1) {
                            System.out.println(
                                    "Time: " + getTime() + " A: Packet " + window[i] + " recevied succesful ack");
                        }
                        updateReceivedTime(window[i].getAcknum(), getTime());
                        adjustBase();
                    }
                } else {
                    for (; i <= packet.getAcknum(); i++) {
                        if (traceLevel == 1) {
                            System.out.println(
                                    "Time: " + getTime() + " A: Packet " + window[i] + " recevied succesful ack");
                        }
                        updateReceivedTime(window[i].getAcknum(), getTime());
                        adjustBase();
                    }
                }
                int c = Math.abs(packet.getAcknum() - base);
                for (; i <= c; i++) {
                    if (traceLevel == 1) {
                        System.out
                                .println("Time: " + getTime() + " A: Packet " + window[i]
                                        + " recevied succesful ack");
                    }
                    updateReceivedTime(window[i].getAcknum(), getTime());
                    adjustBase();

                }

                if (base != nextSeqNum) {
                    startTimer(0, RxmtInterval);
                }
                if (bufferIndex >= 0) {
                    if (traceLevel == 1) {
                        System.out.println("Time: " + getTime() + " A: buffered message " + buffer[bufferIndex]
                                + " sent_to_aOutput");
                    }
                    Message x = new Message(buffer[bufferIndex--].getPayload());
                    aOutput(x);

                }

            } else {
                System.out.println("Time: " + getTime() + " A: Packet " + packet + " recevied out of order ack");
            }

        } else {
            if (traceLevel == 1) {
                System.out.println("Time: " + getTime() + " A: received corrupted ack");
            }
            corruptedPackets++;
        }

    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt() {

        if (traceLevel == 1) {
            System.out.println("Time: " + getTime()
                    + " A: timer interrupt: restarting timer");
        }
        // checks the postion of base and nextSeqNum in window and acts accoridngly
        if (base > nextSeqNum) {
            for (int i = base; i < LimitSeqNo; i++) {
                toLayer3(0, window[i]);
                if (traceLevel == 1) {
                    System.out.println("Time: " + getTime() + " A: Packet " + window[i] + " resent");

                }
                updateSentTime(window[i].getAcknum(), getTime());
                retransmitted_packets++;
            }
            for (int i = 0; i < nextSeqNum; i++) {
                toLayer3(0, window[i]);
                if (traceLevel == 1) {
                    System.out.println("Time: " + getTime() + " A: Packet " + window[i] + " resent");

                }
                updateSentTime(window[i].getAcknum(), getTime());
                retransmitted_packets++;
            }
        } else {
            for (int i = base; i < nextSeqNum; i++) {
                toLayer3(0, window[i]);
                if (traceLevel == 1) {
                    System.out.println("Time: " + getTime() + " A: Packet " + window[i] + " resent");

                }
                updateSentTime(window[i].getAcknum(), getTime());
                retransmitted_packets++;
            }

        }

        startTimer(0, RxmtInterval);

    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        // initializes the window and other neccesary variables for a
        base = FirstSeqNo;
        nextSeqNum = FirstSeqNo;
        window = new Packet[LimitSeqNo];
        buffer = new Packet[LimitSeqNo * LimitSeqNo];
        bufferIndex = -1;
        size = WindowSize;
        original_packets = 0;
        retransmitted_packets = 0;
        messageNumber = 0;
        prev = 0;
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side. "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        // checks the checksum
        if (checkChecksum_receiver(packet)) {
            sentActs++;
            // checks if the expected packet is received
            if (packet.getSeqnum() == expectedSeqNum) {
                toLayer5(packet.getPayload());
                toLayer5Count++;
                lastB = new Packet(0, expectedSeqNum,
                        makeSenderChecksum(0, expectedSeqNum, ""), "");
                toLayer3(1, lastB);

                if (traceLevel == 1) {
                    System.out.println("Time: " + getTime() + " B: Packet " + packet + " received and sending Ack: "
                            + expectedSeqNum);
                }
                if (expectedSeqNum == maxWin - 1) {
                    expectedSeqNum = 0;
                } else {
                    expectedSeqNum++;
                }
            } else {
                if (traceLevel == 1) {
                    System.out.println("Time: " + getTime() + " B: received out of order packet: " + packet);
                }
                if (lastB != null) {
                    toLayer3(1, lastB);
                }

            }
        } else {
            if (traceLevel == 1) {
                System.out.println("Time: " + getTime() + " B: Received corrupted packet");
            }
            corruptedPackets++;
        }

    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        expectedSeqNum = FirstSeqNo;
        maxWin = LimitSeqNo;
        toLayer5Count = 0;
        sentActs = 0;

    }

    // makes checksum for either a or b
    protected static int makeSenderChecksum(int a, int b, String c) {
        Checksum x = new CRC32();
        x.update((a + " " + b + " " + c).getBytes(), 0, (a + " " + b + " " + c).length());
        return (int) x.getValue();
    }

    // checks checksum for either a or b
    protected static boolean checkChecksum_receiver(Packet p) {
        return p.getChecksum() == makeSenderChecksum(p.getSeqnum(), p.getAcknum(), p.getPayload());
    }

    // Use to print final statistics
    protected void Simulation_done() {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIABLE NAMES. DO
        // NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + original_packets);
        System.out.println("Number of retransmissions by A:" + retransmitted_packets);
        System.out.println("Number of data packets delivered to layer 5 at B:" + toLayer5Count);
        System.out.println("Number of ACK packets sent by B:" + sentActs);
        System.out.println("Number of corrupted packets:" + corruptedPackets);
        System.out.println("Ratio of lost packets:" + ((double) (retransmitted_packets - corruptedPackets)
                / (double) (original_packets + retransmitted_packets)));
        System.out.println("Ratio of corrupted packets:" + ((double) (corruptedPackets)
                / (double) (original_packets + retransmitted_packets)));
        System.out.println("Average RTT:" + calcRTT());
        System.out.println("Average communication time:" + calcComT());
        System.out.println("==================================================");

        // EXAMPLE GIVEN BELOW
        // System.out.println("Example statistic you want to check e.g. number of ACK
        // packets received by A :" + "<YourVariableHere>");
    }

}
