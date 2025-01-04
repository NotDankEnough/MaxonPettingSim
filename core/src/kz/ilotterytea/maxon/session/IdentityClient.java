package kz.ilotterytea.maxon.session;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.Timer;
import kz.ilotterytea.maxon.MaxonConstants;
import kz.ilotterytea.maxon.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityClient {
    private final Logger log;

    private final Preferences sessionPreferences;
    private final String clientToken;
    private String accessToken;
    private String userId, username, password;
    private boolean isProcessing, isAuthorised;

    public IdentityClient(Preferences sessionPreferences) {
        startValidationThread();
        this.log = LoggerFactory.getLogger(IdentityClient.class);

        this.clientToken = RandomUtils.generateRandomString();
        this.sessionPreferences = sessionPreferences;
        this.isProcessing = false;
        this.isAuthorised = false;

        if (sessionPreferences.contains("username") && sessionPreferences.contains("password")) {
            this.authorize(sessionPreferences.getString("username"), sessionPreferences.getString("password"));
        }
    }

    public void authorize(String username, String password) {
        log.info("Authorising...");
        this.isProcessing = true;
        sessionPreferences.putString("username", username);
        sessionPreferences.putString("password", password);
        sessionPreferences.flush();

        JsonValue agent = new JsonValue(JsonValue.ValueType.object);
        agent.addChild("name", new JsonValue(String.valueOf(MaxonConstants.GAME_APP_ID.charAt(0)).toUpperCase() + MaxonConstants.GAME_APP_ID.substring(1)));
        agent.addChild("version", new JsonValue(MaxonConstants.GAME_PROTOCOL));

        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("agent", agent);
        json.addChild("username", new JsonValue(username));
        json.addChild("password", new JsonValue(password));
        json.addChild("clientToken", new JsonValue(clientToken));

        Net.HttpRequest request =
                new HttpRequestBuilder()
                        .newRequest()
                        .method(Net.HttpMethods.POST)
                        .url(MaxonConstants.IDENTITY_AUTHENTICATION_URL)
                        .timeout(20000)
                        .header("Content-Type", "application/json")
                        .content(json.toJson(JsonWriter.OutputType.json))
                        .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                IdentityClient.this.isProcessing = false;

                try {
                    JsonValue json = new JsonReader().parse(httpResponse.getResultAsString());

                    if (httpResponse.getStatus().getStatusCode() != HttpStatus.SC_OK) {
                        String type = json.get("error").getString("type");
                        String error = json.get("error").getString("message");
                        log.error("Failed to authorise: {} ({})", error, type);

                        sessionPreferences.remove("username");
                        sessionPreferences.remove("password");
                        sessionPreferences.flush();

                        return;
                    }

                    IdentityClient.this.username = username;
                    IdentityClient.this.password = password;
                    IdentityClient.this.accessToken = json.get("data").getString("accessToken");
                    IdentityClient.this.userId = String.valueOf(json.get("data").get("user").getInt("id"));
                    IdentityClient.this.isAuthorised = true;
                    log.info("Successfully authorised! Welcome back, {}!", IdentityClient.this.username);
                } catch (Exception e) {
                    log.error("An exception was thrown while authorising", e);
                }
            }

            @Override
            public void failed(Throwable t) {
                log.error("Failed to send an authorisation request", t);
            }

            @Override
            public void cancelled() {
                log.info("Authorisation request was cancelled!");
            }
        });
    }

    public void validateToken() {
        if (clientToken == null || accessToken == null) {
            return;
        }

        log.info("Validating token...");

        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("clientToken", new JsonValue(clientToken));
        json.addChild("accessToken", new JsonValue(accessToken));

        Net.HttpRequest request =
                new HttpRequestBuilder()
                        .newRequest()
                        .method(Net.HttpMethods.POST)
                        .url(MaxonConstants.IDENTITY_VALIDATE_URL)
                        .timeout(20000)
                        .header("Content-Type", "application/json")
                        .content(json.toJson(JsonWriter.OutputType.json))
                        .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    JsonValue json = new JsonReader().parse(httpResponse.getResultAsString());

                    if (httpResponse.getStatus().getStatusCode() != HttpStatus.SC_OK) {
                        String type = json.get("error").getString("type");
                        String error = json.get("error").getString("message");
                        log.error("Failed to validate: {} ({})", error, type);
                        accessToken = null;
                        userId = null;
                        isAuthorised = false;
                        authorize(username, password);
                        return;
                    }

                    int expiresInSeconds = json.get("data").getInt("expiresInSeconds");

                    if (expiresInSeconds < 1000) {
                        refreshToken();
                    }

                    log.info("Token validated!");
                } catch (Exception e) {
                    log.error("An exception was thrown while validating", e);
                }
            }

            @Override
            public void failed(Throwable t) {
                log.error("Failed to send a validation request", t);
            }

            @Override
            public void cancelled() {
                log.info("Validation request was cancelled!");
            }
        });
    }

    public void invalidateToken() {
        if (clientToken == null || accessToken == null) {
            return;
        }

        log.info("Invalidating token...");

        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("clientToken", new JsonValue(clientToken));
        json.addChild("accessToken", new JsonValue(accessToken));

        Net.HttpRequest request =
                new HttpRequestBuilder()
                        .newRequest()
                        .method(Net.HttpMethods.POST)
                        .url(MaxonConstants.IDENTITY_INVALIDATE_URL)
                        .timeout(20000)
                        .header("Content-Type", "application/json")
                        .content(json.toJson(JsonWriter.OutputType.json))
                        .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    JsonValue json = new JsonReader().parse(httpResponse.getResultAsString());

                    if (httpResponse.getStatus().getStatusCode() != HttpStatus.SC_OK) {
                        String type = json.get("error").getString("type");
                        String error = json.get("error").getString("message");
                        log.error("Failed to invalidate: {} ({})", error, type);
                        return;
                    }

                    log.info("Invalidated! Bye, {}", username);

                    accessToken = null;
                    userId = null;
                    username = null;
                    password = null;
                    isAuthorised = false;
                    sessionPreferences.remove("username");
                    sessionPreferences.remove("password");
                    sessionPreferences.flush();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void failed(Throwable t) {
            }

            @Override
            public void cancelled() {
            }
        });
    }

    public void refreshToken() {
        if (clientToken == null || accessToken == null) {
            return;
        }

        log.info("Refreshing token...");

        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        json.addChild("clientToken", new JsonValue(clientToken));
        json.addChild("accessToken", new JsonValue(accessToken));

        Net.HttpRequest request =
                new HttpRequestBuilder()
                        .newRequest()
                        .method(Net.HttpMethods.POST)
                        .url(MaxonConstants.IDENTITY_REFRESH_URL)
                        .timeout(20000)
                        .header("Content-Type", "application/json")
                        .content(json.toJson(JsonWriter.OutputType.json))
                        .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    JsonValue json = new JsonReader().parse(httpResponse.getResultAsString());

                    if (httpResponse.getStatus().getStatusCode() != HttpStatus.SC_OK) {
                        String type = json.get("error").getString("type");
                        String error = json.get("error").getString("message");
                        log.error("Failed to refresh: {} ({})", error, type);
                        accessToken = null;
                        userId = null;
                        isAuthorised = false;
                        log.warn(error);
                        return;
                    }

                    accessToken = json.get("data").get("accessToken").asString();
                    log.info("Token has been refreshed!");
                } catch (Exception e) {
                    log.error("An exception was thrown while refreshing", e);
                }
            }

            @Override
            public void failed(Throwable t) {
                log.error("Failed to send a refresh request", t);
            }

            @Override
            public void cancelled() {
                log.info("Refresh request was cancelled!");
            }
        });
    }

    public boolean isAuthorised() {
        return this.isAuthorised;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getUserId() {
        return userId;
    }

    private void startValidationThread() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                validateToken();
            }
        }, 60000, 60000);
    }
}
