package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


/**
 * @author Zhenkai Zhu
 */
public abstract class AbstractStreamTest
{
  protected static final int PORT = 8099;
  protected static final long LARGE_BYTES_NUM = 1024 * 1024 * 1024;
  protected static final long SMALL_BYTES_NUM = 1024 * 1024 * 64;
  protected static final long TINY_BYTES_NUM = 1024 * 64;
  protected static final byte BYTE = 100;
  protected static final long INTERVAL = 20;
  protected HttpServer _server;
  protected TransportClientFactory _clientFactory;
  protected Client _http1Client;
  protected Client _http2Client;
  protected ScheduledExecutorService _scheduler;

  @BeforeClass
  public void setup() throws IOException
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _clientFactory = getClientFactory();
    _http1Client = new TransportClientAdapter(_clientFactory.getClient(getHttp1ClientProperties()), true);
    _http2Client = new TransportClientAdapter(_clientFactory.getClient(getHttp2ClientProperties()), true);
    _server = getServerFactory().createH2cServer(PORT, getTransportDispatcher(), true);
    _server.start();
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    final FutureCallback<None> http1ClientShutdownCallback = new FutureCallback<None>();
    _http1Client.shutdown(http1ClientShutdownCallback);
    http1ClientShutdownCallback.get();

    final FutureCallback<None> http2ClientShutdownCallback = new FutureCallback<None>();
    _http2Client.shutdown(http2ClientShutdownCallback);
    http2ClientShutdownCallback.get();

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    _scheduler.shutdown();
    if (_server != null) {
      _server.stop();
      _server.waitForStop();
    }
  }

  protected Collection<Client> clients()
  {
    return Arrays.asList(_http1Client, _http2Client);
  }

  protected abstract TransportDispatcher getTransportDispatcher();

  protected TransportClientFactory getClientFactory()
  {
    return new HttpClientFactory.Builder().build();
  }

  protected Map<String, String> getHttp1ClientProperties()
  {
    HashMap<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1.name());
    return properties;
  }

  protected Map<String, String> getHttp2ClientProperties()
  {
    HashMap<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2.name());
    return properties;
  }

  protected HttpServerFactory getServerFactory()
  {
    return new HttpServerFactory();
  }

}
