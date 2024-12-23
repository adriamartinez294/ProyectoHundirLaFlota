package com.project;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.concurrent.Task;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;


public class Main extends Application {

    public static UtilsWS wsClient;

    public static String clientId = "";
    public static CtrlConfig ctrlConfig;
    public static CtrlWait ctrlWait;
    public static CtrlPlay ctrlPlay;
    public static CtrlWinner ctrlWinner;
    private static boolean conectado = false;
        
    
        public static void main(String[] args) {
    
            // Iniciar app JavaFX   
            launch(args);
        }
        
        @Override
        public void start(Stage stage) throws Exception {
    
            final int windowWidth = 672;
            final int windowHeight = 460;

            StackPane root = new StackPane();
            UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
            UtilsViews.parentContainer = root;

            UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
            UtilsViews.addView(getClass(), "ViewWait", "/assets/viewWait.fxml");
            UtilsViews.addView(getClass(), "ViewPlay", "/assets/viewPlay.fxml");
            UtilsViews.addView(getClass(), "ViewWinner", "/assets/viewWinner.fxml");
    
            ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
            ctrlWait = (CtrlWait) UtilsViews.getController("ViewWait");
            ctrlPlay = (CtrlPlay) UtilsViews.getController("ViewPlay");
            ctrlWinner = (CtrlWinner) UtilsViews.getController("ViewWinner");
    
            Scene scene = new Scene(UtilsViews.parentContainer);
            
    
            scene.widthProperty().addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) {
                    System.out.println("Width: " + newSceneWidth);
                }
            });
            scene.heightProperty().addListener(new ChangeListener<Number>() {
                @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                    System.out.println("Height: " + newSceneHeight);
                }
            });
            
            stage.setScene(scene);
            stage.onCloseRequestProperty(); // Call close method when closing window
            stage.setTitle("JavaFX - NodeJS");
            stage.setMinWidth(windowWidth);
            stage.setMaxWidth(windowWidth);
            stage.setMinHeight(windowHeight);
            stage.setMaxHeight(windowHeight);
            stage.show();
    
    
    
            stage.widthProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Width changed: " + newVal);
            });
    
            stage.heightProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Height changed: " + newVal);
            });
    
            // Add icon only if not Mac
            if (!System.getProperty("os.name").contains("Mac")) {
                Image icon = new Image("file:/icons/icon.png");
                stage.getIcons().add(icon);
            }
        }
    
        @Override
        public void stop() { 
            if (wsClient != null) {
                wsClient.forceExit();
            }
            System.exit(1); // Kill all executor services
        }
    
        public static void pauseDuring(long milliseconds, Runnable action) {
            PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
            pause.setOnFinished(event -> Platform.runLater(action));
            pause.play();
        }
    
        public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
            List<T> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                T value = clazz.cast(array.get(i));
                list.add(value);
            }
            return list;
        }
    
        public static void connectToServer() {
            if (wsClient != null && conectado) {
                System.out.println("Ya estás conectado.");
                return; // Si ya está conectado, no hacer nada
            }
    
            Platform.runLater(() -> {
                ctrlConfig.txtMessage.setTextFill(Color.WHITE);
                ctrlConfig.txtMessage.setText("Connecting...");
            });
    
            Task<Void> connectionTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        String protocol = ctrlConfig.txtProtocol.getText();
                        String host = ctrlConfig.txtHost.getText();
                        String port = ctrlConfig.txtPort.getText();
    
                        wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);
                        conectado = true; // Marcar como conectado
    
                        wsClient.onMessage(response -> Platform.runLater(() -> wsMessage(response)));
                        wsClient.onError(response -> Platform.runLater(() -> wsError(response)));
                        wsClient.onClose((reason) -> { // El motivo de cierre como parámetro
                            Platform.runLater(() -> {
                                conectado = false;
                                ctrlConfig.txtMessage.setTextFill(Color.RED);
                                ctrlConfig.txtMessage.setText("Connection closed: " + reason); // Muestra el motivo de cierre
                            });
                        });
    
                        Platform.runLater(() -> {
                            ctrlConfig.txtMessage.setTextFill(Color.GREEN);
                            ctrlConfig.txtMessage.setText("Connected successfully");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
    
            new Thread(connectionTask).start();
        }
        
   
    private static void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);
        switch (msgObj.getString("type")) {
            case "clients":
                if (clientId == "") {
                    clientId = msgObj.getString("id");
                }
                if (UtilsViews.getActiveView() == "ViewConfig") {
                    UtilsViews.setViewAnimating("ViewWait");
                }
                List<String> stringList = jsonArrayToList(msgObj.getJSONArray("list"), String.class);
                if (stringList.size() > 0) { ctrlWait.txtPlayer0.setText(stringList.get(0)); }
                if (stringList.size() > 1) { ctrlWait.txtPlayer1.setText(stringList.get(1)); }
                break;
            case "countdown":
                int value = msgObj.getInt("value");
                String txt = String.valueOf(value);
                if (value == 0) {
                    UtilsViews.setViewAnimating("ViewPlay");
                    txt = "GO";
                }
                ctrlWait.txtTitle.setGraphic(null);
                ctrlWait.txtTitle.setAlignment(Pos.CENTER);
                ctrlWait.txtTitle.setText(txt);
                break;
            case "serverMouseMoving":
                ctrlPlay.setPlayersMousePositions(msgObj.getJSONObject("positions"));
                break;
            case "serverSelectableObjects":
                ctrlPlay.setSelectableObjects(msgObj.getJSONObject("selectableObjects"));
                break;
            case "playersReady":
                ctrlPlay.playersReady = true;
                ctrlPlay.waiting.setVisible(false);
                ctrlPlay.playerTurnText.setVisible(true);
                break;
            case "waitingResponse":
                String msg = msgObj.getString("message");
                int col = msgObj.getInt("col");
                int row = msgObj.getInt("row");
                ctrlPlay.playerAction.setText(msg);
                ctrlPlay.checkHitWater(col, row);
                break;
            case "endAttack":
                String land = msgObj.getString("message");
                int col2 = msgObj.getInt("col");
                int row2 = msgObj.getInt("row");

                if (land.equals("hit")){
                    ctrlPlay.fillHit(col2, row2);
                    ctrlPlay.playerAction.setText(ctrlPlay.playerAction.getText() + ". It's a Hit!");
                }
                else{
                    ctrlPlay.fillWater(col2, row2);
                    ctrlPlay.playerAction.setText(ctrlPlay.playerAction.getText() + ". It's Water!");
                }
            case "changeturn":
                try {
                    String player = msgObj.getString("toplayer");
                    ctrlPlay.setPlayerTurn(player);
                    ctrlPlay.playerTurnText.setText("Player turn: " + player);
                    
                } catch (Exception e) {
                    System.err.println("Error retrieving toplayer: " + e.getMessage());
                    System.out.println("Received message: " + msgObj.toString());
                }
                break;
            case "gameover":
                String winner = msgObj.getString("winner");

                ctrlWinner.setWinner(winner);
                UtilsViews.setViewAnimating("ViewWinner");
                
                disconnectFromServer();
                break;
        }
    }

    public static void disconnectFromServer() {
        wsClient.forceExit(); // Cierra la conexión del WebSocket
        wsClient = null; // Limpia la instancia del cliente
        clientId = ""; // Reinicia el ID del cliente
        conectado = false; // Marca como desconectado
        Platform.runLater(() -> {
            ctrlPlay.reset();
            ctrlWait.resetWaitState();
            ctrlConfig.txtMessage.setText("");
        });
    }

    private static void wsError(String response) {
        String connectionRefused = "S'ha refusat la connexió";
        conectado = false;
            Platform.runLater(() -> {
                ctrlConfig.txtMessage.setTextFill(Color.RED);
                ctrlConfig.txtMessage.setText(connectionRefused);
                wsClient = null;
                clientId = "";
                conectado = false;
            });
        }

    public static void restartApplication() {
        Platform.runLater(() -> {
            try {
                Stage currentStage = (Stage) UtilsViews.parentContainer.getScene().getWindow();
                currentStage.close();

                Main app = new Main();
                Stage newStage = new Stage();
                app.start(newStage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public static String getClientId() {
        return clientId;
    }
}
