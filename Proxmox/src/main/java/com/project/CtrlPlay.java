package com.project;

import java.net.URL;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.plaf.basic.BasicOptionPaneUI.ButtonActionListener;

import org.json.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class CtrlPlay implements Initializable {

    @FXML
    private Canvas canvas;

    @FXML
    private Button readyButton;

    @FXML
    public Text waiting;

    private GraphicsContext gc;
    private Boolean showFPS = false;

    private PlayTimer animationTimer;
    private PlayGrid grid;

    public Map<String, JSONObject> clientMousePositions = new HashMap<>();
    private Boolean mouseDragging = false;
    private double mouseOffsetX, mouseOffsetY;
    private double mouseX, mouseY;
    private static String clientId;

    public boolean playersReady = false;

    public static Map<String, JSONObject> selectableObjects = new HashMap<>();
    private String selectedObject = "";

    private WebSocketClient client = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Get drawing context
        this.gc = canvas.getGraphicsContext2D();

        // Set listeners
        UtilsViews.parentContainer.heightProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        UtilsViews.parentContainer.widthProperty().addListener((observable, oldValue, newvalue) -> { onSizeChanged(); });
        
        canvas.setOnMouseMoved(this::setOnMouseMoved);
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);

        // Define grid
        grid = new PlayGrid(150, 25, 25, 10, 10);

        // Start run/draw timer bucle
        animationTimer = new PlayTimer(this::run, this::draw, 0);
        start();
    }


    // When window changes its size
    public void onSizeChanged() {

        double width = UtilsViews.parentContainer.getWidth();
        double height = UtilsViews.parentContainer.getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    // Start animation timer
    public void start() {
        animationTimer.start();
    }

    // Stop animation timer
    public void stop() {
        animationTimer.stop();
    }

    @FXML
    private void setReady(){
        String a = "player " + clientId + " is ready";
        JSONObject message = new JSONObject();
        message.put("type", "ready");
        message.put("message", a);

        client.send(message.toString());

        readyButton.setVisible(false);
        waiting.setVisible(true);
    }


    private void setOnMouseMoved(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        JSONObject newPosition = new JSONObject();
        newPosition.put("x", mouseX);
        newPosition.put("y", mouseY);
        if (grid.isPositionInsideGrid(mouseX, mouseY)) {                
            newPosition.put("col", grid.getCol(mouseX));
            newPosition.put("row", grid.getRow(mouseY));
        } else {
            newPosition.put("col", -1);
            newPosition.put("row", -1);
        }
        clientMousePositions.put(ClientFX.clientId, newPosition);

        JSONObject msgObj = clientMousePositions.get(ClientFX.clientId);
        msgObj.put("type", "clientMouseMoving");
        msgObj.put("clientId", ClientFX.clientId);
    
        if (ClientFX.wsClient != null) {
            ClientFX.wsClient.safeSend(msgObj.toString());
        }
    }

    public static void setClientId(String clientId) {
        CtrlPlay.clientId = clientId;
    }

    private void onMousePressed(MouseEvent event) {

        double mouseX = event.getX();
        double mouseY = event.getY();

        selectedObject = "";
        mouseDragging = false;
        if (!playersReady) {
            for (String objectId : selectableObjects.keySet()) {
                JSONObject obj = selectableObjects.get(objectId);
                int objX = obj.getInt("x");
                int objY = obj.getInt("y");
                int cols = obj.getInt("cols");
                int rows = obj.getInt("rows");
    
                if (isPositionInsideObject(mouseX, mouseY, objX, objY,  cols, rows)) {
                    if (event.isPrimaryButtonDown() && obj.getString("player").equals(this.clientId)) {
                        selectedObject = objectId;
                        mouseDragging = true;
                        mouseOffsetX = event.getX() - objX;
                        mouseOffsetY = event.getY() - objY;
                        break;
                    }
    
                    else if (event.isSecondaryButtonDown()) {
                        obj.put("cols", rows);
                        obj.put("rows", cols);
        
                        for (String objectid2 : selectableObjects.keySet()) {
                            JSONObject selectableObject = selectableObjects.get(objectId);
                            drawSelectableObject(objectId, selectableObject);
                        }
    
                        break;
                    }
                }
            }
        }

        else if (playersReady) {
            int col = grid.getCol(mouseX);
            int row = grid.getRow(mouseY);


            fillWater(col, row);
        }
    }  
    
    private void fillWater(int col, int row) {
    
        // Log the col and row to ensure they are valid
        System.out.println("Col: " + col + ", Row: " + row);

        if (col < 0 || row < 0) {
            System.out.println("Error: posición fuera de la cuadrícula.");
            return;
        }
    
        double cellX = grid.getCellX(col);
        double cellY = grid.getCellY(row);
    
        // Log the calculated positions
        System.out.println("Cell X: " + cellX + ", Cell Y: " + cellY);
    
        // Ensure grid.getCellSize() is valid
        double cellSize = grid.getCellSize();
        System.out.println("Cell Size: " + cellSize);
    
        // Check if cellSize is greater than 0
        if (cellSize <= 0) {
            System.out.println("Error: Cell size must be greater than 0.");
            return;
        }

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
        // Draw light blue rectangle for water
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(cellX, cellY, cellSize, cellSize);
    
        // Draw black border
        gc.setStroke(Color.BLACK);
        gc.strokeRect(cellX, cellY, cellSize, cellSize);
    }

    private void onMouseDragged(MouseEvent event) {
        if (mouseDragging) {
            JSONObject obj = selectableObjects.get(selectedObject);
            double objX = event.getX() - mouseOffsetX;
            double objY = event.getY() - mouseOffsetY;
            
            obj.put("x", objX);
            obj.put("y", objY);
            obj.put("col", grid.getCol(objX));
            obj.put("row", grid.getRow(objY));

            JSONObject msgObj = selectableObjects.get(selectedObject);
            msgObj.put("type", "clientSelectableObjectMoving");
            msgObj.put("objectId", obj.getString("objectId"));
        
            if (ClientFX.wsClient != null) {
                ClientFX.wsClient.safeSend(msgObj.toString());
            }
        }
        setOnMouseMoved(event);
    }

    private void onMouseReleased(MouseEvent event) {
        if (selectedObject != "") {
            JSONObject obj = selectableObjects.get(selectedObject);
            int objCol = obj.getInt("col");
            int objRow = obj.getInt("row");

            if (objCol != -1 && objRow != -1) {
                obj.put("x", grid.getCellX(objCol));
                obj.put("y", grid.getCellY(objRow));
            }

            JSONObject msgObj = selectableObjects.get(selectedObject);
            msgObj.put("type", "clientSelectableObjectMoving");
            msgObj.put("objectId", obj.getString("objectId"));
        
            if (ClientFX.wsClient != null) {
                ClientFX.wsClient.safeSend(msgObj.toString());
            }

            mouseDragging = false;
            selectedObject = "";
        }
    }


    public void setPlayersMousePositions(JSONObject positions) {
        clientMousePositions.clear();
        for (String clientId : positions.keySet()) {
            JSONObject positionObject = positions.getJSONObject(clientId);
            clientMousePositions.put(clientId, positionObject);
        }
    }

    public void setSelectableObjects(JSONObject objects) {
        selectableObjects.clear();
        for (String objectId : objects.keySet()) {
            JSONObject positionObject = objects.getJSONObject(objectId);
            selectableObjects.put(objectId, positionObject);
        }
    }

    public Boolean isPositionInsideObject(double positionX, double positionY, int objX, int objY, int cols, int rows) {
        double cellSize = grid.getCellSize();
        double objectWidth = cols * cellSize;
        double objectHeight = rows * cellSize;

        double objectLeftX = objX;
        double objectRightX = objX + objectWidth;
        double objectTopY = objY;
        double objectBottomY = objY + objectHeight;

        return positionX >= objectLeftX && positionX < objectRightX &&
               positionY >= objectTopY && positionY < objectBottomY;
    }

    // Run game (and animations)
    private void run(double fps) {

        if (animationTimer.fps < 1) { return; }

        // Update objects and animations here
    }

    public void setClient() {
        this.client = UtilsWS.getClient();
    }

    // Draw game to canvas
    public void draw() {

        setClientId(ClientFX.getClientId());
        setClient();

        // Clean drawing area
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw colored 'over' cells

        for (String clientId : clientMousePositions.keySet()) {
            JSONObject position = clientMousePositions.get(clientId);

            int col = position.getInt("col");
            int row = position.getInt("row");

            // Comprovar si està dins dels límits de la graella
            if (row >= 0 && col >= 0) {
                if ("A".equals(clientId)) {
                    gc.setFill(Color.LIGHTBLUE); 
                } else {
                    gc.setFill(Color.LIGHTGREEN); 
                }
                // Emplenar la casella amb el color clar
                gc.fillRect(grid.getCellX(col), grid.getCellY(row), grid.getCellSize(), grid.getCellSize());
            }
        }

        // Draw grid
        drawGrid();

        // Draw selectable objects
        for (String objectId : selectableObjects.keySet()) {
            JSONObject selectableObject = selectableObjects.get(objectId);
            if (selectableObject.getString("player").equals(clientId)){
                drawSelectableObject(objectId, selectableObject);
            }
        }

        // Draw mouse circles
        for (String clientId : clientMousePositions.keySet()) {
            JSONObject position = clientMousePositions.get(clientId);
            if ("A".equals(clientId)) {
                gc.setFill(Color.BLUE);
            } else {
                gc.setFill(Color.GREEN); 
            }
            gc.fillOval(position.getInt("x") - 5, position.getInt("y") - 5, 10, 10);
        }

        // Draw FPS if needed
        if (showFPS) { animationTimer.drawFPS(gc); }   
    }

    public void drawGrid() {
        gc.setStroke(Color.BLACK);

        for (int row = 0; row < grid.getRows(); row++) {
            for (int col = 0; col < grid.getCols(); col++) {
                double cellSize = grid.getCellSize();
                double x = grid.getStartX() + col * cellSize;
                double y = grid.getStartY() + row * cellSize;
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }

    public void drawSelectableObject(String objectId, JSONObject obj) {
        double cellSize = grid.getCellSize();

        int x = obj.getInt("x");
        int y = obj.getInt("y");
        double width = obj.getInt("cols") * cellSize;
        double height = obj.getInt("rows") * cellSize;

        // Seleccionar un color basat en l'objectId
        Color color;
        switch (objectId.toLowerCase()) {
            case "red":
                color = Color.RED;
                break;
            case "blue":
                color = Color.BLUE;
                break;
            case "green":
                color = Color.GREEN;
                break;
            case "yellow":
                color = Color.YELLOW;
                break;
            default:
                color = Color.GRAY;
                break;
        }

        // Dibuixar el rectangle
        gc.setFill(color);
        gc.fillRect(x, y, width, height);

        // Dibuixar el contorn
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y, width, height);

        // Opcionalment, afegir text (per exemple, l'objectId)
        gc.setFill(Color.BLACK);
        gc.fillText(objectId, x + 5, y + 15);
    }
}
