package client.CT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import model.protocol.Packet;
import model.protocol.RequestType;

/**
 * 서버와의 소켓 연결을 관리한다. 요청-응답은 requestId로 짝을 맞춰 블로킹으로 기다리고,
 * 서버가 먼저 보내는 푸시 패킷은 등록된 PushListener로 전달한다. 배관 코드라 전부 구현되어 있음.
 */
public class ServerConnection {
    private static final long RESPONSE_TIMEOUT_SECONDS = 10;

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Map<String, BlockingQueue<Packet>> pendingResponses = new ConcurrentHashMap<>();
    private volatile PushListener pushListener;

    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // 서버 쪽(ClientHandler)과 대칭: ObjectOutputStream을 먼저 만들고 flush한 뒤
        // ObjectInputStream을 만들어야 양쪽 모두 스트림 헤더를 기다리다 멈추지 않는다.
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        Thread readerThread = new Thread(this::readLoop, "server-connection-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void setPushListener(PushListener listener) {
        this.pushListener = listener;
    }

    /** 요청을 보내고, 대응하는 응답 Packet이 올 때까지 블로킹으로 기다린다. */
    public Packet sendRequest(Packet request) {
        BlockingQueue<Packet> queue = new ArrayBlockingQueue<>(1);
        pendingResponses.put(request.getRequestId(), queue);
        try {
            sendPacket(request);
            Packet response = queue.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response == null) {
                throw new IllegalStateException("서버 응답 시간 초과: " + request.getType());
            }
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("요청 전송 실패: " + request.getType(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("응답 대기 중 인터럽트됨", e);
        } finally {
            pendingResponses.remove(request.getRequestId());
        }
    }

    public void disconnect() {
        try {
            sendPacket(Packet.request(RequestType.DISCONNECT, null));
        } catch (IOException ignored) {
        }
        closeQuietly();
    }

    private void sendPacket(Packet packet) throws IOException {
        synchronized (out) {
            out.writeObject(packet);
            // 직렬화 캐시(handle) 때문에 같은 객체를 재전송해도 예전 상태가 나가는 것을 방지
            out.reset();
            out.flush();
        }
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                Packet packet = (Packet) in.readObject();
                if (packet == null) {
                    break;
                }
                if (packet.isPush()) {
                    if (pushListener != null) {
                        pushListener.onPush(packet);
                    }
                } else {
                    BlockingQueue<Packet> queue = pendingResponses.get(packet.getRequestId());
                    if (queue != null) {
                        queue.offer(packet);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // 연결 종료
        } finally {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
