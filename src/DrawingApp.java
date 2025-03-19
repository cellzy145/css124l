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
            canvasPanel.zoom(1.1);
        } else if (e.getSource() == zoomOutItem) {
            //zoom out by 0.9
            canvasPanel.zoom(0.9);
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


    public static void main(String[] args) {
        new DrawingApp();
    }
}
