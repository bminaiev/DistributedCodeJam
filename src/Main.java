import java.math.BigInteger;
import java.util.*;

public class Main {
    final boolean debug = false;

    // [partFrom .. partTo)
    int partFrom, partTo;
    int partLen;
    int n, m;

    void initPart() {
        int partSize = 1 + (n - 1) / nodes;
        partFrom = Math.min(n, partSize * myId);
        partTo = Math.min(n, partSize * (myId + 1));
        partLen = partTo - partFrom;
    }

    void removeNodes() {
        int partSize = 1 + (n - 1) / nodes;
        while (partSize * (nodes - 1) >= n) {
            nodes--;
        }
    }

    final int p = 239017;
    final int len = (1 << 6);
    int[] pow = new int[len + 1];
    final int PARTS = 1005;
    final int mod = 1023456817;

    int mul(int x, int y) {
        return (int)(x*1L*y%mod);
    }

    int sub(int x, int y) {
        x -= y;
        return x < 0? (x + mod) : x;
    }

    int add(int x, int y) {
        x += y;
        return x >= mod ? (x - mod) : x;
    }

    class MiddleFinder {
        int[] values = new int[PARTS];
        int minElement, maxElement;
        int partSize;
        int cntLess, cntMore;

        void clear(int minElement, int maxElement) {
            this.minElement = minElement;
            this.maxElement = maxElement;
            cntLess = 0;
            cntMore = 0;
            partSize = 1 + (maxElement - minElement) / PARTS;
            Arrays.fill(values, 0);
        }


        MiddleFinder(int minElement, int maxElement) {
            clear(minElement, maxElement);
        }

        void addElement(int x) {
            if (x < minElement) {
                cntLess++;
            } else if (x > maxElement) {
                cntMore++;
            } else {
                values[(x - minElement) / partSize]++;
            }
        }

    }
    MiddleFinder finder = new MiddleFinder(0, (int) 1e9);

    class Solver {
        long startPos;
        int curHash;
        int[] circle = new int[len];
        int circleIt;
        int totalLen;
        int allSame = -1;

        void clear() {
            totalLen = 0;
            curHash = 0;
            for (int i = 0; i < len; i++) {
                getNewValue(false);
            }
        }

        Solver(long startPos) {
            this.startPos = startPos;
            clear();
        }

        void getNewValue(boolean addToCounter) {
            int value = median.GetData(startPos + totalLen);
            addToCircle(value);
            if (addToCounter) {
                finder.addElement(value);
            } else {
                if (allSame == -1 || allSame == value) {
                    allSame = value;
                } else {
                    allSame = -2;
                }
            }
        }

        void addToCircle(int value) {
            if (totalLen >= circle.length) {
                curHash = sub(curHash, mul(pow[len - 1],  circle[circleIt]));
            }
            totalLen++;
            circle[circleIt] = value;
            circleIt = (circleIt + 1) & (len - 1);
            curHash = add(mul(curHash, p),  value);
        }
    }

    void solve() {
        pow[0] = 1;
        for (int i = 1; i < pow.length; i++) {
            pow[i] = mul(pow[i - 1], p);
        }
        Random rnd = new Random(7877881 * (myId + 1));
        final int SOLVERS = 1;
        Solver[] solvers = new Solver[SOLVERS];
        HashSet<Integer> hashes = new HashSet<>();
        n = median.GetN();
        initPart();
        int allSame = -1;
        for (int i = 0; i < SOLVERS; i++) {
            solvers[i] = new Solver(Math.abs(rnd.nextLong()) % (long) 1e17);
//            System.err.println("start from " + partFrom);
            if (solvers[i].allSame > 0) {
                allSame = solvers[i].allSame;
            }
            if (hashes.contains(solvers[i].curHash)) {
                solvers[i] = null;
            } else {
                hashes.add(solvers[i].curHash);
            }
        }
        for (int i = 0; i < nodes; i++) {
            if (i == myId) {
                continue;
            }
            putInt(i, hashes.size());
            for (int x : hashes) {
                putInt(i, x);
            }
            sendMessage(i);
        }
        HashSet<Integer> otherHashes = new HashSet<>();
        for (int i = 0; i < nodes; i++) {
            if (i == myId) {
                for (int j = 0; j < solvers.length; j++) {
                    if (solvers[j] != null && otherHashes.contains(solvers[j].curHash)) {
                        solvers[j] = null;
                    }
                }
            } else {
                receiveMessage(i);
                int sz = readInt(i);
                for (int j = 0; j < sz; j++) {
                    otherHashes.add(readInt(i));
                }
            }
        }
        hashes.addAll(otherHashes);
        int[] hashesArray = new int[hashes.size()];
        int it = 0;
        for (int i : hashes) {
            hashesArray[it++] = i;
        }
        Arrays.sort(hashesArray);
        for (int globIter = 0; globIter < 3; globIter++) {
            for (Solver s : solvers) {
                if (s == null) {
                    continue;
                }
                s.clear();
//                System.err.println("partLen = " + partLen);
                for (int i = 0; ; i++) {
                    s.getNewValue(true);
                    if (Arrays.binarySearch(hashesArray, s.curHash) >= 0) {
                        break;
                    }
                }
            }
            putInt(rootNode, finder.cntLess);
            for (int i = 0; i < PARTS; i++) {
                putInt(rootNode, finder.values[i]);
            }
            putInt(rootNode, finder.cntMore);
            sendMessage(rootNode);
            if (iAmRoot()) {
                int sumCntLess = 0, sum = 0;
                int[] partsSum = new int[PARTS];
                for (int i = 0; i < nodes; i++) {
                    receiveMessage(i);
                    sumCntLess += readInt(i);
                    for (int j = 0; j < PARTS; j++) {
                        partsSum[j] += readInt(i);
                    }
                    sum += readInt(i);
                }
                for (int i = 0; i < PARTS; i++) {
                    sum += partsSum[i];
                }
                sum += sumCntLess;
//                if (median.GetN() % sum) {
//                    throw new AssertionError();
//                }
                int nextLeft = -1, nextRight = -1;
//                System.err.println(sumCntLess + " " +Arrays.toString(partsSum));
                for (int i = 0; i < PARTS; i++) {
                    sumCntLess += partsSum[i];
                    if (sumCntLess * 2 > sum) {
                        nextLeft = finder.minElement + finder.partSize * i;
                        nextRight = nextLeft + finder.partSize - 1;
                        break;
                    }
                }
                if (nextLeft == -1) {
                    throw new AssertionError();
                }
//                System.err.println("!!! " + nextLeft + " " + nextRight);
                for (int i = 0; i < nodes; i++) {
                    putInt(i, nextLeft);
                    putInt(i, nextRight);
                    sendMessage(i);
                }
            }
            receiveMessage(rootNode);
            finder.clear(readInt(rootNode), readInt(rootNode));
        }
        if (finder.minElement != finder.maxElement) {
            throw new AssertionError(finder.minElement + " " + finder.maxElement);
        }
        putInt(rootNode, allSame);
        sendMessage(rootNode);
        if (iAmRoot()) {
            for (int i = 0; i < nodes; i++) {
                receiveMessage(i);
                int x =readInt(i);
                if (x > 0) {
                    allSame = x;
                }
            }
            if (allSame > 0) {
                println(allSame);
            } else {
                println(finder.minElement);
            }
        }
    }

    void putInt(int to, int value) {
        if (debug) {
            System.err.println("send int " + value + " to " + to);
        }
        message.PutInt(to, value);
    }

    void putLong(int to, long value) {
        if (debug) {
            System.err.println("send long " + value + " to " + to);
        }
        message.PutLL(to, value);
    }

    void sendMessage(int to) {
        if (debug) {
            System.err.println("send message to " + to);
        }
        message.Send(to);
    }

    int readInt(int from) {
        if (debug) {
            System.err.println("receive int from " + from);
        }
        return message.GetInt(from);
    }

    long readLong(int from) {
        if (debug) {
            System.err.println("receive long from " + from);
        }
        return message.GetLL(from);
    }

    void receiveMessage(int from) {
        if (debug) {
            System.err.println("receive message from " + from);
        }
        message.Receive(from);
    }

    void println(String s) {
        System.out.println(s);
    }

    void println(long val) {
        System.out.println(val);
    }

    int[] joinIntArray(int value) {
        putInt(rootNode, value);
        sendMessage(rootNode);
        if (iAmRoot()) {
            int[] res = new int[nodes];
            for (int i = 0; i < nodes; i++ ){
                receiveMessage(i);
                res[i] = readInt(i);
            }
            return res;
        } else {
            return null;
        }
    }

    long[] joinLongArray(long value) {
        putLong(rootNode, value);
        sendMessage(rootNode);
        if (iAmRoot()) {
            long[] res = new long[nodes];
            for (int i = 0; i < nodes; i++ ){
                receiveMessage(i);
                res[i] = readLong(i);
            }
            return res;
        } else {
            return null;
        }
    }

    boolean iAmRoot() {
        return myId == rootNode;
    }
    int myId;
    int nodes;
    int rootNode = 0;

    void start() {
        myId = message.MyNodeId();
        nodes = message.NumberOfNodes();
        solve();
    }

    public static void main(String[] args) {
        new Main().start();
    }
}
