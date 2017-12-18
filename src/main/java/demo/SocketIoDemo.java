package demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;

public class SocketIoDemo {

    public static void main(String[] args) throws Exception {
        String basePath = args.length > 0 ? args[0] : "http://localhost:8090";
        System.out.println("Initializing IO client. BasePath: " + basePath);
        showMessages("entered args: " + Arrays.toString(args));

        String auth = args.length > 1 ? args[1] : null;

        SocketIoDemo demo = new SocketIoDemo(basePath, auth);
        demo.start();
    }

    private String defAuth = "Bearer xxx";
    private String mAuth;
    private String mBaseUrl;
    private List<String> mCookie;

    public SocketIoDemo(String baseUrl, String auth) {
        if (auth == null || auth.length() <= 0) {
            auth = defAuth;
        }
        mAuth = auth;
        mBaseUrl = baseUrl;
    }

    public void start() {

        IO.Options opts = new IO.Options();
        opts.path = "/v1/realtime-location/socket.io";

        try {
            Socket socket = IO.socket(mBaseUrl, opts);
            socket.on(Socket.EVENT_CONNECT, new LogIOListener(Socket.EVENT_CONNECT))
                    .on("customer-location-changed", new LogIOListener("customer-location-changed"))
                    .on(Socket.EVENT_DISCONNECT, new LogIOListener(Socket.EVENT_DISCONNECT))
                    .on(Socket.EVENT_CONNECTING, new LogIOListener(Socket.EVENT_CONNECTING))
                    .on(Socket.EVENT_PING, new LogIOListener(Socket.EVENT_PING))
                    .on(Socket.EVENT_PONG, new LogIOListener(Socket.EVENT_PONG))
                    .on(Socket.EVENT_CONNECT_ERROR, new LogIOListener(Socket.EVENT_CONNECT_ERROR))
                    .on(Socket.EVENT_CONNECT_TIMEOUT, new LogIOListener(Socket.EVENT_CONNECT_TIMEOUT))
                    .on(Socket.EVENT_ERROR, new LogIOListener(Socket.EVENT_ERROR))
                    .on(Socket.EVENT_RECONNECTING, new LogIOListener(Socket.EVENT_RECONNECTING))
                    .on(Socket.EVENT_MESSAGE, new LogIOListener(Socket.EVENT_MESSAGE));

            socket.io()
                    .on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Transport transport = (Transport) args[0];
                            // Adding headers when EVENT_REQUEST_HEADERS is called
                            transport
                                    .on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            showMessages(Transport.EVENT_REQUEST_HEADERS + ", args: " + Arrays.toString(args));
                                            Map<String, List<String>> mHeaders = (Map<String, List<String>>) args[0];
                                            mHeaders.put("Authorization", Arrays.asList(mAuth));
                                            if (mCookie != null) {
                                                mHeaders.put("cookie", mCookie);
                                            }
                                            showMessages(Transport.EVENT_REQUEST_HEADERS + ", headers: " + mHeaders);
                                        }
                                    })
                                    .on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            showMessages(Transport.EVENT_RESPONSE_HEADERS + ", args: " + Arrays.toString(args));
                                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                                            mCookie = headers.get("set-cookie");
                                        }
                                    })
                                    .on(Transport.EVENT_ERROR, new LogIOListener(Transport.EVENT_ERROR));
                        }
                    })
                    .on(Manager.EVENT_PACKET, new LogIOListener(Manager.EVENT_PACKET));

            socket.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class LogIOListener implements Emitter.Listener {
        private String tag;

        public LogIOListener(String tag) {
            this.tag = tag;
        }

        @Override
        public void call(Object... args) {
            showMessages(tag + ", args: " + Arrays.toString(args));
        }
    }

    private static void showMessages(final String message) {
        System.out.println("message: " + message);
    }
}
