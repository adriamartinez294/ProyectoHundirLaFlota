package com.project;


import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PlayGrid {

    private final double startX;
    private final double startY;
    private final double cellSize;
    private final int cols;
    private final int rows;

    public PlayGrid(double startX, double startY, double cellSize, int rows, int cols) {
        this.startX = startX;
        this.startY = startY;
        this.cellSize = cellSize;
        this.cols = cols;
        this.rows = rows;
    }

    public boolean isPositionInsideGrid(double x, double y) {
        return x >= startX && x < startX + cols * cellSize &&
               y >= startY && y < startY + rows * cellSize;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getCellSize() {
        return cellSize;
    }

    public double getCols() {
        return cols;
    }

    public double getRows() {
        return rows;
    }

    public int getCol(double x) {
        if (x < startX || x >= startX + cols * cellSize) {
            return -1; // Fuera de la cuadrícula
        }
        return (int) ((x - startX) / cellSize);
    }

    public int getRow(double y) {
        if (y < startY || y >= startY + rows * cellSize) {
            return -1; // Fuera de la cuadrícula
        }
        return (int) ((y - startY) / cellSize);
    }

    public int getCellX(int col) {
        return (int) (getStartX() + col * getCellSize());
    }

    public int getCellY(int row) {
        return (int) (getStartY() + row * getCellSize());
    }

    public void drawColumnNumbers(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.setFont(new Font(12));  // Tamaño de fuente para los números

        // Dibujar los números sobre las columnas
        for (int col = 0; col < cols; col++) {
            double x = startX + col * cellSize + cellSize / 2;
            double y = startY - 5;  // Colocar los números justo arriba de la cuadrícula
            gc.fillText(String.valueOf(col + 1), x, y);
        }
    }

    public void drawRowLetters(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.setFont(new Font(12));  // Tamaño de fuente para las letras
    
        // Dibujar las letras a la izquierda de las filas
        for (int row = 0; row < rows; row++) {
            double x = startX - 20;  // Colocar las letras a la izquierda de la cuadrícula (ajusta -20 según el espacio deseado)
            double y = startY + row * cellSize + cellSize / 2;
            gc.fillText(String.valueOf((char) ('A' + row)), x, y);
        }
    }

}
