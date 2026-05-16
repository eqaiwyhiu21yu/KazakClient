package me.aidan.sydney.proxy;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standalone Minecraft TCP proxy server.
 * Forwards client connections to a target Minecraft server.
 * Run with: java PingBypassProxy <targetHost> <targetPort> [listenPort]
 */
public class PingBypassProxy {
    private final String targetHost;
    private final int targetPort;
    private final int listenPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private int connectionId = 0;

    public PingBypassProxy(String targetHost, int targetPort, int listenPort) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.listenPort = listenPort;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(listenPort);
        running.set(true);
        System.out.println("[PingBypassProxy] Listening on 0.0.0.0:" + listenPort);
        System.out.println("[PingBypassProxy] Forwarding to " + targetHost + ":" + targetPort);

        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                int id = ++connectionId;
                threadPool.submit(() -> handleConnection(id, clientSocket));
            } catch (SocketException e) {
                if (!running.get()) break;
                System.err.println("[PingBypassProxy] Accept error: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[PingBypassProxy] Accept error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        threadPool.shutdownNow();
        System.out.println("[PingBypassProxy] Stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getListenPort() {
        return listenPort;
    }

    private void handleConnection(int id, Socket clientSocket) {
        System.out.println("[PingBypassProxy:" + id + "] Client connected from " + clientSocket.getRemoteSocketAddress());
        try (
                Socket client = clientSocket;
                DataInputStream clientIn = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                ByteArrayOutputStream handshakeBuf = new ByteArrayOutputStream();
                DataOutputStream handshakeOut = new DataOutputStream(handshakeBuf)
        ) {
            readVarInt(clientIn);
            int packetId = readVarInt(clientIn);

            if (packetId != 0) {
                System.out.println("[PingBypassProxy:" + id + "] Expected handshake (id=0), got id=" + packetId + ". Closing.");
                return;
            }

            writeVarInt(handshakeOut, 0);
            int protocolVersion = readVarInt(clientIn);
            writeVarInt(handshakeOut, protocolVersion);

            String origAddr = readString(clientIn);
            writeString(handshakeOut, targetHost);

            int port = clientIn.readUnsignedShort();
            handshakeOut.writeShort(targetPort);

            int nextState = readVarInt(clientIn);
            writeVarInt(handshakeOut, nextState);

            System.out.println("[PingBypassProxy:" + id + "] Handshake: protocol=" + protocolVersion
                    + ", addr=" + origAddr + ":" + port
                    + " -> " + targetHost + ":" + targetPort
                    + ", state=" + nextState);

            Socket targetSocket = new Socket(targetHost, targetPort);
            System.out.println("[PingBypassProxy:" + id + "] Connected to target " + targetHost + ":" + targetPort);

            DataOutputStream targetOut = new DataOutputStream(new BufferedOutputStream(targetSocket.getOutputStream()));
            DataInputStream targetIn = new DataInputStream(new BufferedInputStream(targetSocket.getInputStream()));

                byte[] rewrittenHandshake = handshakeBuf.toByteArray();
                writeVarInt(targetOut, rewrittenHandshake.length);
                targetOut.write(rewrittenHandshake);
            targetOut.flush();

            if (nextState == 1) {
                forwardStatus(id, clientIn, client.getOutputStream(), targetIn, targetOut);
            } else {
                forwardBidirectional(id, client, clientIn, targetSocket, targetIn, targetOut);
            }
        } catch (IOException e) {
            System.out.println("[PingBypassProxy:" + id + "] Connection closed: " + e.getMessage());
        }
    }

    private void forwardStatus(int id, DataInputStream clientIn, OutputStream clientOut,
                               DataInputStream targetIn, OutputStream targetOut) throws IOException {
        Thread clientThread = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = clientIn.read(buf)) != -1) {
                    targetOut.write(buf, 0, n);
                    targetOut.flush();
                }
            } catch (IOException ignored) {}
        });
        clientThread.start();

        byte[] buf = new byte[8192];
        int n;
        while ((n = targetIn.read(buf)) != -1) {
            clientOut.write(buf, 0, n);
            clientOut.flush();
        }
        clientThread.interrupt();
    }

    private void forwardBidirectional(int id, Socket client, DataInputStream clientIn,
                                      Socket target, DataInputStream targetIn, DataOutputStream targetOut) throws IOException {
        Thread clientReader = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = clientIn.read(buf)) != -1) {
                    targetOut.write(buf, 0, n);
                    targetOut.flush();
                }
            } catch (IOException ignored) {}
        });
        clientReader.start();

        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = targetIn.read(buf)) != -1) {
                client.getOutputStream().write(buf, 0, n);
                client.getOutputStream().flush();
            }
        } catch (IOException ignored) {}
        try { target.close(); } catch (IOException ignored) {}
        try { client.close(); } catch (IOException ignored) {}
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int result = 0, shift = 0;
        while (true) {
            int b = in.readByte() & 0xFF;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.writeByte(value);
                return;
            }
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    private String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void writeString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java " + PingBypassProxy.class.getName() + " <targetHost> <targetPort> [listenPort]");
            System.out.println("  targetHost  - The Minecraft server to proxy to");
            System.out.println("  targetPort  - The port of the Minecraft server");
            System.out.println("  listenPort  - Local port to listen on (default: 25565)");
            return;
        }

        String host = args[0];
        int tPort, lPort = 25565;
        try {
            tPort = Integer.parseInt(args[1]);
            if (args.length > 2) lPort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
            return;
        }

        PingBypassProxy proxy = new PingBypassProxy(host, tPort, lPort);
        Runtime.getRuntime().addShutdownHook(new Thread(proxy::stop));
        try {
            proxy.start();
        } catch (IOException e) {
            System.err.println("Failed to start proxy: " + e.getMessage());
        }
    }
}
