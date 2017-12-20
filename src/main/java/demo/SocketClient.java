package demo;


import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import io.socket.engineio.client.transports.Polling;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

public class SocketClient {
    private static final String TAG = "sockets.SocketClient";

    private volatile Socket mSocket;
    private final String mBaseUrl;
    private final String mPath;

    private String mAuthKey;

    private volatile List<String> mCookie;

    public SocketClient(String baseUrl, String path) {
        mBaseUrl = baseUrl;
        mPath = path;

        init();
    }

    public void init() {
        OkHttpClient client = getHttpClient(null);

        IO.Options opt = setUpOptions(client);

        initIOConnection(opt);
    }

    public void start(CharSequence authKey) {
        // TODO
        mAuthKey = authKey.toString();

        if (mSocket != null) {
            if (mSocket.connected()) {
                return;
            }
            mSocket.connect();
        }
    }

    public void stop() {
        if (mSocket != null) {
            SocketIoDemo.showMessages("stop requested by the client");
            mSocket.disconnect();
        }
    }

    private String getAuthKey() {
        return "Bearer " + mAuthKey;
    }

    private List<String> getCookie() {
        return mCookie;
    }

    private void saveCookie(List<String> cookie) {
        mCookie = cookie;
    }

    private OkHttpClient getHttpClient(Interceptor interceptor) {
        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        if (interceptor != null) {
            okBuilder.addInterceptor(interceptor);
        }
        okBuilder.connectTimeout(0, TimeUnit.NANOSECONDS)
                .readTimeout(0, TimeUnit.NANOSECONDS)
                .followRedirects(true);

        return okBuilder.build();
    }

    private IO.Options setUpOptions(OkHttpClient okClient) {
        IO.setDefaultOkHttpCallFactory(okClient);
        IO.setDefaultOkHttpWebSocketFactory(okClient);

        IO.Options opt = new IO.Options();
        opt.path = mPath;
        opt.callFactory = okClient;
        opt.webSocketFactory = okClient;

        opt.transports = new String[]{Polling.NAME};
        opt.forceNew = true;
        opt.reconnection = false;
        opt.timeout = -1;
        opt.query = "transport=polling";

        return opt;
    }

    private void initIOConnection(IO.Options options) {
        try {
            mSocket = IO.socket(mBaseUrl, options);

            mSocket.on(Socket.EVENT_CONNECT, new SocketIoDemo.LogIOListener(Socket.EVENT_CONNECT))
                    .on("customer-location-changed", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            SocketIoDemo.showMessages("customer-location-changed, args: " + Arrays.asList(args));
                        }
                    })
                    .on(Socket.EVENT_DISCONNECT, new SocketIoDemo.LogIOListener(Socket.EVENT_DISCONNECT))
                    .on(Socket.EVENT_CONNECTING, new SocketIoDemo.LogIOListener(Socket.EVENT_CONNECTING))
                    .on(Socket.EVENT_PING, new SocketIoDemo.LogIOListener(Socket.EVENT_PING))
                    .on(Socket.EVENT_PONG, new SocketIoDemo.LogIOListener(Socket.EVENT_PONG))
                    .on(Socket.EVENT_CONNECT_ERROR, new SocketIoDemo.LogIOListener(Socket.EVENT_CONNECT_ERROR))
                    .on(Socket.EVENT_CONNECT_TIMEOUT, new SocketIoDemo.LogIOListener(Socket.EVENT_CONNECT_TIMEOUT))
                    .on(Socket.EVENT_ERROR, new SocketIoDemo.LogIOListener(Socket.EVENT_ERROR))
                    .on(Socket.EVENT_RECONNECTING, new SocketIoDemo.LogIOListener(Socket.EVENT_RECONNECTING))
                    .on(Socket.EVENT_MESSAGE, new SocketIoDemo.LogIOListener(Socket.EVENT_MESSAGE));

            mSocket.io()
                    .on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Transport transport = (Transport) args[0];
                            // Adding headers when EVENT_REQUEST_HEADERS is called
                            transport
                                    .on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
                                            Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                                            if (!headers.containsKey("Authorization")) {
                                                headers.put("Authorization", Arrays.asList(getAuthKey()));
                                            }
                                            List<String> cookie = getCookie();
                                            if (cookie != null) {
                                                headers.put("Cookie", cookie);
                                            }
//                                            SocketIoDemo.showMessages(Manager.EVENT_TRANSPORT + ":" + Transport.EVENT_REQUEST_HEADERS + ", args: " + Arrays.toString(args));
                                        }
                                    })
                                    .on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                                        @Override
                                        public void call(Object... args) {
//                                            SocketIoDemo.showMessages(Manager.EVENT_TRANSPORT + ":" + Transport.EVENT_REQUEST_HEADERS + ", args: " + Arrays.toString(args));
                                            Map<String, List<String>> header = (Map<String, List<String>>) args[0];
                                            saveCookie(header.get("set-cookie"));
                                        }
                                    })
                                    .on(Transport.EVENT_OPEN, new SocketIoDemo.LogIOListener(Manager.EVENT_TRANSPORT + ":" + Transport.EVENT_OPEN))
                                    .on(Transport.EVENT_ERROR, new SocketIoDemo.LogIOListener(Manager.EVENT_TRANSPORT + ":" + Transport.EVENT_ERROR));
                        }
                    }).on(Manager.EVENT_PACKET, new SocketIoDemo.LogIOListener(Manager.EVENT_PACKET));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
