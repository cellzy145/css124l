import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class DrawingApp extends JFrame implements ActionListener, ChangeListener {
    //frame size/dimensions
    private int frameWidth = 1300;
    private int frameHeight = 1300;

    //main panels/components
    private JPanel ribbonPanel;
    private DrawingPanel canvasPanel;
    private JMenuBar menuBar;

    //JMenus
    private JMenu fileMenu, editMenu, helpMenu;

    //fileMenu items
    private JMenuItem newItem, saveItem, openItem;
    private JMenu autoSaveMenu;

    //editMenu items
    private JMenuItem undoItem, redoItem, zoomInItem, zoomOutItem, clearItem;

    //helpMenu items
    private JMenuItem aboutItem, controlsItem; //list shortcut keys on controls item

    //timer for autosave
    private Timer autoSaveTimer;

    //ribbonPanel elements
    JButton newButton, clearButton, undoButton, redoButton, colorButton;
    JComboBox<String> brushTypeCombo;
    JSlider brushSizeSlider;
    ImageIcon newIcon, undoIcon, redoIcon;

    //constructor
    public DrawingApp() {
        //initialize JFrame
        this.setTitle("Simple Drawing App");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(frameWidth, frameHeight);
        this.setLayout(new BorderLayout());

        //initialize main components
        menuBar = new JMenuBar();
        ribbonPanel = new JPanel();
        canvasPanel = new DrawingPanel();

        //initialize and JMenus to menuBar
        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        helpMenu = new JMenu("Help");

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        //intiialize and add JMenuItems to fileMenu
        newItem = new JMenuItem("New");
        saveItem = new JMenuItem("Save");
        openItem = new JMenuItem("Open");
        autoSaveMenu = new JMenu("Enable Autosave");

        //create items for autoSaveMenu and add listeners (better this way)
        String[] intervals = {"0s", "1s", "5s", "10s", "30s", "60s"};
        for (String interval : intervals) {
            JMenuItem intervalItem = new JMenuItem(interval);
            intervalItem.addActionListener(e -> setAutoSaveInterval(Integer.parseInt(interval.replace("s", "")) * 1000));
            autoSaveMenu.add(intervalItem);
        }

        //set mnemonic and accelerators for fileMenuItems
        newItem.setMnemonic(KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        openItem.setMnemonic(KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));

        //add items to fileMenu
        fileMenu.add(newItem);
        fileMenu.add(saveItem);
        fileMenu.add(openItem);
        fileMenu.add(autoSaveMenu);

        //initialize and add JMenuItems to editMenu
        undoItem = new JMenuItem("Undo");
        redoItem = new JMenuItem("Redo");
        zoomInItem = new JMenuItem("Zoom In");
        zoomOutItem = new JMenuItem("Zoom Out");
        clearItem = new JMenuItem("Clear");

        //set mnemonic and accelerators for editMenuItems
        undoItem.setMnemonic(KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        redoItem.setMnemonic(KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK));
        zoomInItem.setMnemonic(KeyEvent.VK_EQUALS); //I have no numpad
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK));
        zoomOutItem.setMnemonic(KeyEvent.VK_MINUS);
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
        clearItem.setMnemonic(KeyEvent.VK_C);
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK));

        //add items to editMenu
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.add(zoomInItem);
        editMenu.add(zoomOutItem);
        editMenu.add(clearItem);

        //initialize and add JMenuItems to helpMenu (no need for mnemonics and accelerators?)
        aboutItem = new JMenuItem("About"); //TODO -- JDialog about program
        controlsItem = new JMenuItem("Shortcut Keys"); //TODO -- JDialog about controls

        helpMenu.add(aboutItem);
        helpMenu.add(controlsItem);

        //create ribbonPanel
        ribbonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        //initialize ribbon components
        //TODO -- change text to images
        newIcon = getScaledIcon("src/new-document.png", 28, 28);
        undoIcon = getScaledIcon("src/undo.png", 28, 28);
        redoIcon = getScaledIcon("src/redo.png", 28, 28);

        newButton = new JButton(newIcon);
        clearButton = new JButton("Clear");
        undoButton = new JButton(undoIcon);
        redoButton = new JButton(redoIcon);
        colorButton = new JButton("Color");
        brushTypeCombo = new JComboBox<>(new String[]{"Pencil", "Eraser"});
        brushSizeSlider = new JSlider(1, 50, canvasPanel.getBrushSize());

        //remove the focus border appearing after button is clicked
        newButton.setFocusPainted(false);
        clearButton.setFocusPainted(false);
        undoButton.setFocusPainted(false);
        redoButton.setFocusPainted(false);
        colorButton.setFocusPainted(false);

        newButton.setPreferredSize(new Dimension(32, 32));
        undoButton.setPreferredSize(new Dimension(32, 32));
        redoButton.setPreferredSize(new Dimension(32, 32));

        ribbonPanel.add(newButton);
        ribbonPanel.add(undoButton);
        ribbonPanel.add(redoButton);
        ribbonPanel.add(clearButton);
        ribbonPanel.add(colorButton);
        ribbonPanel.add(brushTypeCombo);
        ribbonPanel.add(brushSizeSlider);

        //add main components
        this.setJMenuBar(menuBar);
        this.add(ribbonPanel, BorderLayout.NORTH);
        this.add(canvasPanel, BorderLayout.CENTER);

        //add action listeners
        newItem.addActionListener(this);
        saveItem.addActionListener(this);
        openItem.addActionListener(this);
        autoSaveMenu.addActionListener(this);
        undoItem.addActionListener(this);
        redoItem.addActionListener(this);
        zoomInItem.addActionListener(this);
        zoomOutItem.addActionListener(this);
        clearItem.addActionListener(this);
        aboutItem.addActionListener(this);
        controlsItem.addActionListener(this);
        newButton.addActionListener(this);
        clearButton.addActionListener(this);
        undoButton.addActionListener(this);
        redoButton.addActionListener(this);
        colorButton.addActionListener(this);
        brushTypeCombo.addActionListener(this);

        //user change listener for brush size
        brushSizeSlider.addChangeListener(this);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //switch better?
        if (e.getSource() == newItem || e.getSource() == newButton) {
            //create new canvas
            createNewCanvas();
        } else if (e.getSource() == saveItem) {
            //save canvas (use imagebuffer to because i dont want serialization)
            canvasPanel.saveImage(true);
        } else if (e.getSource() == openItem) {
            //open saved canvas
            canvasPanel.openImage();
        } else if (e.getSource() == undoItem || e.getSource() == undoButton) {
            //undo using stack maybe
            canvasPanel.undo();
        } else if (e.getSource() == redoItem || e.getSource() == redoButton) {
            //redo from undo stack
            canvasPanel.redo();
        } else if (e.getSource() == zoomInItem) {
            //zoom in by 1.1
            canvasPanel.zoomIn();
        } else if (e.getSource() == zoomOutItem) {
            //zoom out by 0.9
            canvasPanel.zoomOut();
        } else if (e.getSource() == clearItem || e.getSource() == clearButton) {
            //clear current canvas
            canvasPanel.clearCanvas();
        } else if (e.getSource() == aboutItem) {
            //message dialog about program
            String aboutMessage = "Simple Drawing App\n" +
                    "Version 1.0\n" +
                    "Author: Jian Brent Jumaquio";
            JOptionPane.showMessageDialog(this, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getSource() == controlsItem) {
            //controls dialog
            String controlsMessage = "Shortcut Keys:\n" +
                    "Ctrl + N: New Canvas\n" +
                    "Ctrl + S: Save Image\n" +
                    "Ctrl + O: Open Image\n" +
                    "Ctrl + Z: Undo\n" +
                    "Ctrl + Y: Redo\n" +
                    "Ctrl + =: Zoom In\n" +
                    "Ctrl + -: Zoom Out\n" +
                    "Ctrl + Delete: Clear Canvas\n" +
                    "Middle Mouse Button: Pan\n" +
                    "Ctrl + Mouse Wheel: Zoom";
            JOptionPane.showMessageDialog(this, controlsMessage, "Controls", JOptionPane.INFORMATION_MESSAGE);
        } else if (e.getSource() == colorButton) {
            Color newColor = JColorChooser.showDialog(null, "Choose Brush Color", canvasPanel.getBrushColor());
            if (newColor != null) {
                canvasPanel.setBrushColor(newColor);
            }
        } else if (e.getSource() == brushTypeCombo) {
            String selectedBrush = (String) brushTypeCombo.getSelectedItem();
            canvasPanel.setBrushType(selectedBrush);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == brushSizeSlider) {
            int newSize = brushSizeSlider.getValue();
            canvasPanel.setBrushSize(newSize);
        }
    }

    //timer
    private void setAutoSaveInterval(int interval) {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }
        if (interval > 0) {
            autoSaveTimer = new Timer(interval, e -> canvasPanel.saveImage(false));
            autoSaveTimer.start();
        }
    }

    //new canvas with custom dimensions
    private void createNewCanvas() {
        //create text fields with current canvas dimensions as default values
        JTextField widthField = new JTextField(String.valueOf(canvasPanel.getCanvasWidth()));
        JTextField heightField = new JTextField(String.valueOf(canvasPanel.getCanvasHeight()));

        //create panel to hold input fields
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Width:"));
        panel.add(widthField);
        panel.add(new JLabel("Height:"));
        panel.add(heightField);

        //show dialog and wait for user input
        int result = JOptionPane.showConfirmDialog(this, panel, "Set Canvas Size", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                //parse user input
                int newWidth = Integer.parseInt(widthField.getText());
                int newHeight = Integer.parseInt(heightField.getText());

                //validate input
                if (newWidth > 0 && newHeight > 0) {
                    //resize canvas and clear previous content
                    canvasPanel.resizeCanvas(newWidth, newHeight);
                } else {
                    JOptionPane.showMessageDialog(this, "Width and height must be positive integers.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid integers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //scale icons
    private ImageIcon getScaledIcon(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImg);
    }


    //USE BUFFER IMAGE!!!
    class DrawingPanel extends JPanel {
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

                    //make sure file has png extension
                    String filePath = fileToSave.getAbsolutePath();
                    if (!filePath.toLowerCase().endsWith(".png")) {
                        fileToSave = new File(filePath + ".png");
                    }

                    try {
                        //save image to file
                        ImageIO.write(image, "PNG", fileToSave);
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

        public void zoomIn() {
            zoomFactor *= 1.1;
            repaint();
        }

        public void zoomOut() {
            zoomFactor *= 0.9;
            repaint();
        }

        //do undo and redo first for seamless implementasyon
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

    public static void main(String[] args) {
        new DrawingApp();
    }
}
