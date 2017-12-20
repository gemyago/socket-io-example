package demo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.socket.emitter.Emitter;

public class SocketIoDemo {

    private static final String defAuth = "Bearer xxx";
    private static final String path = "/v1/realtime-location/socket.io";

    public static void main(String[] args) throws Exception {
        String basePath = args.length > 0 ? args[0] : "http://localhost:8090";
        System.out.println("Initializing IO client. BasePath: " + basePath);
        showMessages("entered args: " + Arrays.toString(args));

        String auth = args.length > 1 ? args[1] : null;

        String duration = args.length > 2 ? args[2] : null;
        long lifeTime = 0;
        if (duration != null && duration.length() > 0) {
            lifeTime = Long.parseLong(duration);
        }

        if (auth == null || auth.length() <= 0) {
            auth = defAuth;
        }

        SocketClient demoClient = new SocketClient(basePath, path);

        if (lifeTime > 0) {
            runTaskAsync(demoClient, auth);
            Thread.sleep(lifeTime);
            demoClient.stop();
        } else {
            demoClient.start(auth);
        }
    }

    private static void runTaskAsync(final SocketClient client, final String auth) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                client.start(auth);
            }
        });
        thread.start();
    }

    public static class LogIOListener implements Emitter.Listener {
        private String tag;

        public LogIOListener(String tag) {
            this.tag = tag;
        }

        @Override
        public void call(Object... args) {
            showMessages(tag + ", args: " + Arrays.toString(args));
        }
    }

    static DateFormat dateFormat;

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void showMessages(final String message) {
        String now = dateFormat.format(new Date());
        System.out.println("[" + now + "] message: " + message);
    }
}
