package demo;

import io.socket.client.*;
import io.socket.emitter.*;
import org.json.*;

public class SocketIoDemo {
  public static void main(String[] args) throws Exception {
    String basePath = args.length > 0 ? args[0] : "http://localhost:8090";
    System.out.println("Initializing IO client. BasePath: " + basePath);
    
    IO.Options opts = new IO.Options();
    opts.path = "/v1/realtime-location/socket.io";
    Socket socket = IO.socket(basePath, opts);
    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        System.out.println("Connected");
      }
    }).on("customer-location-changed", new Emitter.Listener() {
      @Override
      public void call(Object... args) {
        System.out.println("customer-location-changed");
        // JSONObject data = (JSONObject) args[0];
        System.out.println(args[0]);
      }
    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

      @Override
      public void call(Object... args) {
        System.out.println("Disconnected");
      }

    });
    socket.connect();
  }
}
