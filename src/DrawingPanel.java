import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

//USE BUFFER IMAGE!!!
public class DrawingPanel extends JPanel {
    private BufferedImage image;
    private Graphics2D graphics;

    //default values for brush
    private int brushSize = 5;
    private Color brushColor = Color.BLACK;
    private String brushType = "Pencil";
    private int lastX = -1, lastY =  -1;

    //undo and redo
    private Stack<BufferedImage> undoStack = new Stack<>();
    private Stack<BufferedImage> redoStack = new Stack<>();

    //zoom and pan
    private double zoomFactor = 1.0;
    private int offsetX = 0, offsetY = 0;
    private boolean isPanning = false;
    private int lastMouseX, lastMouseY;

    //canvas parameters
    private int padding = 50; //background/workspace/padding/etc..
    private Color paddingColor = Color.GRAY;
    private int canvasWidth = 1100;
    private int canvasHeight = 1100;

    //store last saved file path for autosave
    private File lastSavedFile;

    //constructor for DrawingPanel
    public DrawingPanel() {
        //set canvas size and color
        setPreferredSize(new Dimension(canvasWidth, canvasHeight));
        setBackground(Color.WHITE);

        //create image buffer for drawing
        image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();

        //clear the canvas to initialize the panel
        clearCanvas();

        //add mouse listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    //start panning when mmb is pressed
                    isPanning = true;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    //save the current state for undo/redo
                    saveState();

                    //get the starting position of drawing, adjusted for zoom and panning
                    lastX = (int) ((e.getX() - offsetX) / zoomFactor) - padding;
                    lastY = (int) ((e.getY() - offsetY) / zoomFactor) - padding;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    //stop panning when mmb is released
                    isPanning = false;
                }
            }
        });

        //mouse wheel listener for zooming
        addMouseWheelListener(e -> {
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                double oldZoom = zoomFactor;

                //zoom with mouse wheel
                if (e.getWheelRotation() < 0) {
                    zoomFactor *= 1.1; //zoom in
                } else {
                    zoomFactor *= 0.9; //zoom out
                }

                //zoom at pointer
                int mouseX = e.getX();
                int mouseY = e.getY();
                offsetX = (int) (mouseX - (mouseX - offsetX) * (zoomFactor / oldZoom));
                offsetY = (int) (mouseY - (mouseY - offsetY) * (zoomFactor / oldZoom));

                repaint();
            }
        });

        //mouse motion listener for dragging events
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    //update offset based on mouse movement while panning
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;

                    offsetX += dx;
                    offsetY += dy;

                    //panning limit
                    int maxOffsetX = getWidth() / 2;
                    int minOffsetX = -(int) ((canvasWidth + 2 * padding) * zoomFactor - getWidth() / 2);
                    int maxOffsetY = getHeight() / 2;
                    int minOffsetY = -(int) ((canvasHeight + 2 * padding) * zoomFactor - getHeight() / 2);

                    offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, offsetX));
                    offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, offsetY));

                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    //handle drawing when dragging with the left mouse button
                    int x = (int) ((e.getX() - offsetX) / zoomFactor) - padding;
                    int y = (int) ((e.getY() - offsetY) / zoomFactor) - padding;

                    //set canvas boundaries as limit on drawing
                    if (x >= 0 && x < canvasWidth && y >= 0 && y < canvasHeight) {
                        if (lastX >= 0 && lastY >= 0 && lastX < canvasWidth && lastY < canvasHeight) {
                            // Set brush color and erase
                            if (brushType.equals("Eraser")) {
                                graphics.setColor(Color.WHITE);
                            } else {
                                graphics.setColor(brushColor);
                            }
                            graphics.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            graphics.drawLine(lastX, lastY, x, y);
                        }
                        lastX = x;
                        lastY = y;
                        repaint();
                    } else {
                        //reset last position once mouse leaves canvas
                        lastX = -1;
                        lastY = -1;
                    }
                }
            }
        });

        //component listener for window resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                centerCanvas();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        //draw padding around the canvas (dead space)
        g2d.setColor(paddingColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        //apply zoom and panning transformation here
        g2d.translate(offsetX, offsetY);
        g2d.scale(zoomFactor, zoomFactor);

        //draw the canvas background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(padding, padding, canvasWidth, canvasHeight);

        //clip drawing to canvas area (strokes musn't overshoot)
        g2d.setClip(padding, padding, canvasWidth, canvasHeight);

        //draw image content onto the canvas
        g2d.drawImage(image, padding, padding, null);

        //reset transformations to avoid affecting other ui elements
        g2d.dispose();
    }

    public void centerCanvas() {
        //get canvas centered offset
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        //calculate scaled canvas size with padding
        int scaledCanvasWidth = (int) ((canvasWidth + 2 * padding) * zoomFactor);
        int scaledCanvasHeight = (int) ((canvasHeight + 2 * padding) * zoomFactor);

        //center canvas
        offsetX = (panelWidth - scaledCanvasWidth) / 2;
        offsetY = (panelHeight - scaledCanvasHeight) / 2;
    }

    public void clearCanvas() {
        saveState();

        //fill canvas
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvasWidth, canvasHeight);

        //restore brush color and repaint canvas
        graphics.setColor(brushColor);
        repaint();
    }

    //resize canvas
    public void resizeCanvas(int width, int height) {
        //save state for undo function
        saveState();

        //blank image with user specified dimensions
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D newGraphics = newImage.createGraphics();

        //clear canvas
        clearCanvas();

        //update canvas parameters
        canvasWidth = width;
        canvasHeight = height;
        image = newImage;
        graphics = newGraphics;

        //reset zoom and pan
        zoomFactor = 1.0;
        offsetX = 0;
        offsetY = 0;

        //center canvas
        centerCanvas();

        repaint();
    }

    public void saveImage(boolean showDialog) {
        if (showDialog){
            //use file chooser for saving the image
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Image");
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();

                //make sure file has PNG extension
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".png")) {
                    fileToSave = new File(filePath + ".png");
                }

                try {
                    //MAKE WHITE BACKGROUND!!!
                    //create a copy of image with white background instead
                    BufferedImage imageToSave = new BufferedImage(
                            canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = imageToSave.createGraphics();

                    //fill background with white
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, canvasWidth, canvasHeight);

                    //draw strokes onto the white background
                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();

                    //can remove code above if wanted is transparent background

                    //save image to file
                    ImageIO.write(image, "PNG", fileToSave);

                    //store saved file path
                    this.lastSavedFile = fileToSave;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else { //auto save - use documents folder
            if (lastSavedFile != null) {
                try {
                    //logic if the file has been manually saved (save a copy with filename and " - autosave" appended)
                    //same location too
                    String originalPath = lastSavedFile.getAbsolutePath();
                    String autoSavePath = originalPath.replace(".png", " - autosave.png");
                    File autoSaveFile = new File(autoSavePath);

                    //do the same thing with normal save
                    BufferedImage imageToSave = new BufferedImage(
                            canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = imageToSave.createGraphics();

                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, canvasWidth, canvasHeight);

                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();

                    //save new image to auto-save file
                    ImageIO.write(imageToSave, "PNG", autoSaveFile);
                    System.out.println("Auto-save file saved successfully: " + autoSaveFile.getAbsolutePath()); //debug
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                //if the file hasn't been manually saved then save to documents folder
                String documentsPath = System.getProperty("user.home") + "/Documents";
                File documentsFolder = new File(documentsPath);

                //check if documents folder exist first
                if (!documentsFolder.exists()) {
                    documentsFolder.mkdirs(); //create the folder if it doesn't exist
                }

                //save in documents folder
                File autoSaveFile = new File(documentsFolder, "autosave.png");

                System.out.println("Auto-save file path: " + autoSaveFile.getAbsolutePath()); //debug

                try {
                    //do the same thing with normal save
                    BufferedImage imageToSave = new BufferedImage(
                            canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = imageToSave.createGraphics();

                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, canvasWidth, canvasHeight);

                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();

                    //save new image to auto-save file
                    ImageIO.write(imageToSave, "PNG", autoSaveFile);
                    System.out.println("Auto-save file saved successfully: " + autoSaveFile.getAbsolutePath()); //debug
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public void openImage() {
        //open a file chooser to select image file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                //load selected image to drawing panel
                image = ImageIO.read(fileToOpen);
                graphics = image.createGraphics();
                repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            //save current state to redo stack and restore last state
            redoStack.push(image);
            image = undoStack.pop();
            graphics = image.createGraphics();
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()){
            //save current state to undo stack and restorethe last redo state
            undoStack.push(image);
            image = redoStack.pop();
            graphics = image.createGraphics();
            repaint();
        }
    }

    public void zoom(double zoom) {
        zoomFactor *= zoom;
        repaint();
    }

    //do undo and redo first
    private void saveState() {
        //save a copy of the current canvas for undo functionality (get a canvas snapshot)
        BufferedImage copy = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        undoStack.push(copy);
        redoStack.clear();
    }

    //getters and setters
    public int getCanvasWidth() {
        return canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    public Color getBrushColor() {
        return brushColor;
    }

    public void setBrushColor(Color brushColor) {
        this.brushColor = brushColor;
    }

    public String getBrushType() {
        return brushType;
    }

    public void setBrushType(String brushType) {
        this.brushType = brushType;
    }
}