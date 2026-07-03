/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The <code>ChatAgentClient</code> class communicates with the CoastWatch
 * Help Agent service over HTTP.  A client is created with the service base URL
 * and then used to create a server-side chat session for a specified
 * file-search store and model.  Once the session is created, messages may be
 * streamed through that session and the session may be deleted when it is no
 * longer needed.
 *
 * <p>Message responses are delivered to a {@link StreamListener} as status
 * text, error text, token text, or a completion signal.  The underlying service
 * requests and responses are encoded as JSON, and streamed message responses
 * are read from Server-Sent Events.</p>
 *
 * <p>The class reports HTTP and JSON processing failures as
 * <code>IOException</code>.  A missing streaming session is reported using
 * {@link SessionNotFoundException}.</p>
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class ChatAgentClient {

  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private final String serviceUrl;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new help-agent client.
   *
   * @param serviceUrl the help-agent service base URL
   */
  public ChatAgentClient (
    String serviceUrl
  ) {

    this.serviceUrl = serviceUrl;

  } // ChatAgentClient ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new service session and returns its ID.
   *
   * @param softwareVersion the CoastWatch Utilities software version
   *
   * @return the created session ID
   *
   * @throws IOException if the session request fails
   */
  public String createSessionForSoftwareVersion (
    String softwareVersion
  ) throws IOException {

    // Create a JSON request payload for the session creation API
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put ("softwareVersion", softwareVersion);
    return (createSession (payload));

  } // createSessionForSoftwareVersion

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new service session and returns its ID.
   *
   * @param storeName the file-search store name
   * @param model the model name
   *
   * @return the new service session ID
   *
   * @throws IOException if the session request fails
   */
  public String createSession (
    String storeName,
    String model
  ) throws IOException {

    // Create a JSON request payload for the session creation API
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put ("storeName", storeName);
    payload.put ("model", model);
    return (createSession (payload));

  } // createSession

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new service session from a request payload.
   *
   * @param payload the session creation fields
   *
   * @return the created session ID
   *
   * @throws IOException if the session request fails
   */
  private String createSession (
    Map<String, Object> payload
  ) throws IOException {

    String json = JSON_MAPPER.writeValueAsString (payload);

    // Send the request and read the response body
    HttpURLConnection conn = openJsonPost (
      serviceUrl + "/api/v1/chat/sessions",
      "application/json"
    );
    writeBody (conn, json);

    int code = conn.getResponseCode();
    String body = readResponseBody (conn);
    conn.disconnect();

    // Check for a successful response from the service
    if (code / 100 != 2) {
      throw new IOException ("Failed to create session. HTTP " + code + ": " + body);
    } // if

    // Parse the returned session identifier
    SessionResponse response = parseJson (body, SessionResponse.class);
    if (response == null || response.sessionId == null || response.sessionId.isEmpty()) {
      throw new IOException ("Session ID missing from response: " + body);
    } // if

    return (response.sessionId);

  } // createSession

  ////////////////////////////////////////////////////////////

  /**
   * Deletes a session ID if it exists.
   *
   * @param sessionId the session ID to delete
   *
   * @throws IOException if the request fails
   */
  public void deleteSession (
    String sessionId
  ) throws IOException {

    HttpURLConnection conn = (HttpURLConnection)
      new URL (serviceUrl + "/api/v1/chat/sessions/" + sessionId).openConnection();
    conn.setRequestMethod ("DELETE");
    conn.setConnectTimeout (15000);
    conn.setReadTimeout (15000);
    conn.getResponseCode();
    conn.disconnect();

  } // deleteSession

  ////////////////////////////////////////////////////////////

  /**
   * Sends a message using a session and streams the response chunks back to
   * a listener.
   *
   * @param sessionId the session ID to use
   * @param message the message to send
   * @param listener the listener to receive streamed events, possibly null
   *
   * @throws IOException if the streaming request fails
   */
  public void streamMessage (
    String sessionId,
    String message,
    StreamListener listener
  ) throws IOException {

    // Create a JSON request payload for the message streaming API
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put ("message", message);
    String json = JSON_MAPPER.writeValueAsString (payload);

    // Open the streaming connection and write the request
    HttpURLConnection conn = openJsonPost (
      serviceUrl + "/api/v1/chat/sessions/" + sessionId + "/messages/stream",
      "text/event-stream"
    );
    writeBody (conn, json);

    // Check the HTTP response before consuming the event stream
    int code = conn.getResponseCode();
    if (code == 404) {
      String body = readResponseBody (conn);
      conn.disconnect();
      throw new SessionNotFoundException ("Session not found: " + body);
    } // if

    if (code / 100 != 2) {
      String body = readResponseBody (conn);
      conn.disconnect();
      throw new IOException ("Streaming request failed. HTTP " + code + ": " + body);
    } // if

    // Read SSE data lines until the service signals completion
    try (BufferedReader reader = new BufferedReader (
      new InputStreamReader (conn.getInputStream(), StandardCharsets.UTF_8)
    )) {

      String line;
      while ((line = reader.readLine()) != null) {

        if (!line.startsWith ("data:")) {
          continue;
        } // if

        // Parse the chunk payload and route it to the listener
        String jsonData = line.substring (5).trim();
        StreamPayload payloadData = parseJson (jsonData, StreamPayload.class);
        String type = (payloadData == null || payloadData.type == null) ? "" : payloadData.type;
        String text = (payloadData == null || payloadData.text == null) ? "" : payloadData.text;

        if (listener != null) {
          switch (type) {
            case "status":
              listener.onStatus (text);
              break;
            case "error":
              listener.onError (text);
              break;
            case "token":
              listener.onToken (text);
              break;
            default:
              break;
          } // switch
        } // if

        // Stop reading once the service sends a terminal chunk
        if (payloadData != null && payloadData.done) {
          if (listener != null) {
            listener.onDone();
          } // if
          break;
        } // if

      } // while

    } // try
    finally {
      conn.disconnect();
    } // finally

  } // streamMessage

  ////////////////////////////////////////////////////////////

  /**
   * Opens a JSON POST connection to the service.
   *
   * @param url the target URL
   * @param accept the accepted response type
   *
   * @return the configured connection
   *
   * @throws IOException if the connection cannot be opened
   */
  private HttpURLConnection openJsonPost (
    String url,
    String accept
  ) throws IOException {

    HttpURLConnection conn = (HttpURLConnection) new URL (url).openConnection();
    conn.setRequestMethod ("POST");
    conn.setDoOutput (true);
    conn.setConnectTimeout (15000);
    conn.setReadTimeout (0);
    conn.setRequestProperty ("Content-Type", "application/json");
    conn.setRequestProperty ("Accept", accept);

    return (conn);

  } // openJsonPost

  ////////////////////////////////////////////////////////////

  /**
   * Writes a request body to an open connection.
   *
   * @param conn the connection to write to
   * @param body the body text
   *
   * @throws IOException if the write fails
   */
  private static void writeBody (
    HttpURLConnection conn,
    String body
  ) throws IOException {

    byte[] bytes = body.getBytes (StandardCharsets.UTF_8);
    conn.setFixedLengthStreamingMode (bytes.length);

    try (OutputStream out = conn.getOutputStream()) {
      out.write (bytes);
    } // try

  } // writeBody

  ////////////////////////////////////////////////////////////

  /**
   * Reads a response body from a connection.
   *
   * @param conn the connection to read from
   *
   * @return the response body, possibly empty
   *
   * @throws IOException if the body cannot be read
   */
  private static String readResponseBody (
    HttpURLConnection conn
  ) throws IOException {

    // Prefer the error stream when available, otherwise use the input stream
    InputStream stream = conn.getErrorStream();
    if (stream == null) {
      try {
        stream = conn.getInputStream();
      } // try
      catch (IOException e) {
        return ("");
      } // catch
    } // if

    if (stream == null) {
      return ("");
    } // if

    // Read the entire response body into one string
    try (BufferedReader reader = new BufferedReader (
      new InputStreamReader (stream, StandardCharsets.UTF_8)
    )) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append (line);
      } // while
      return (builder.toString());
    } // try

  } // readResponseBody

  ////////////////////////////////////////////////////////////

  /**
   * Parses a JSON string into a typed object.
   *
   * @param json the JSON string
   * @param type the destination type
   *
   * @return the parsed object
   *
   * @throws IOException if parsing fails
   */
  private static <T> T parseJson (
    String json,
    Class<T> type
  ) throws IOException {

    return (JSON_MAPPER.readValue (json, type));

  } // parseJson

  ////////////////////////////////////////////////////////////

  /**
   * Minimal session-creation response payload.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class SessionResponse {
    public String sessionId;
  } // SessionResponse

  ////////////////////////////////////////////////////////////

  /**
   * Minimal streaming payload from the service.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class StreamPayload {
    public String type;
    public String text;
    public boolean done;
  } // StreamPayload

  ////////////////////////////////////////////////////////////

  /**
   * Listener for streamed service events.
   */
  public interface StreamListener {

    /**
     * Handles a status message from the service.
     *
     * @param text the status message text.
     */
    void onStatus (String text);

    /**
     * Handles an error message from the service.
     *
     * @param text the error message text.
     */
    void onError (String text);

    /**
     * Handles a response text token from the service.
     *
     * @param text the response text token.
     */
    void onToken (String text);

    /** Handles the end of the streamed response. */
    void onDone ();

  } // StreamListener

  ////////////////////////////////////////////////////////////

  /**
   * Thrown when the service reports that a session no longer exists.
   */
  public static final class SessionNotFoundException extends IOException {

    /**
     * Creates a new session not found exception.
     *
     * @param message the exception message.
     */
    public SessionNotFoundException (
      String message
    ) {

      super (message);

    } // SessionNotFoundException ctor

  } // SessionNotFoundException

  ////////////////////////////////////////////////////////////

} // ChatAgentClient
