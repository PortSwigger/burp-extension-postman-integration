package assess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import assess.model.RequestSource;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IRequestInfo;

public class PostmanFrame extends JFrame implements MouseMotionListener, MouseListener {

    private static final long serialVersionUID = -3439093083112839349L;
    /**
     * Start X of ScrollPane
     */
    private static final int REQ_PANE_X = 10;
    /**
     * Start Y of ScrollPane
     */
    private static final int REQ_PANE_Y = 170;
    /**
     * Width of ScrollPane
     */
    private static final int REQ_PANE_WIDTH = 900;
    /**
     * Height of ScrollPane
     */
    private static final int REQ_PANE_HEIGHT = 250;
    /**
     * Minimum Space between right side of reqScrollpane and that of this pane.
     */
    private static final int MINIMUM_SPACE_OF_REQPANE_RIGHTSIDE = 100;
    private static String encoding = "UTF-8";
    /**
     * Triangle Label for resize
     */
    JLabel triangleLabel;
    /**
     * resize flag. If true, reqScrollPane can be resized
     */
    boolean resizeFlg = false;
    private JPanel mainPanel;
    private JLabel encodingLabel;
    private JTextField encodingTextField;
    private Label titleLabel;
    private JLabel noteLabel;
    private JLabel colNameLabel;
    private JTextField colNameTextField;
    private JLabel folderNameLabel;
    private JTextField folderNameTextField;
    private JButton setFolderNameBtn;
    private JButton autoSetFolderNameBtn;
    private JButton removeDuplicateURLBtn;
    private JButton exportBtn;
    private JLabel headerNoteLabel;
    private JScrollPane reqScrollPane;
    private List<IHttpRequestResponse> reqList;
    private IExtensionHelpers helpers;
    private IBurpExtenderCallbacks callbacks;
    private int reqCount;
    /**
     * Table for Requests
     */
    private JTable requestsTable;
    /**
     * Table Model
     */
    private PostmanTableModel tableModel;

    public PostmanFrame(int posX, int posY, int reqCount) {
        this.setBounds(posX, posY, 1200, 540);
        java.net.URL iconUrl = getClass().getClassLoader().getResource("TG-16-16.png");
        this.setIconImage(new ImageIcon(iconUrl).getImage());

        this.reqCount = reqCount;

        render();
    }

    private String getParentPath(String url) throws MalformedURLException {
        String path = new URL(url).getPath();
        return path.substring(0, path.lastIndexOf('/'));
    }

    private String getFileName(String url) {
        return url.substring(url.lastIndexOf('/') + 1, url.length());
    }

    private void render() {
        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.addMouseMotionListener(this);
        mainPanel.addMouseListener(this);
        titleLabel = new Label("Postman Integration Setting");
        titleLabel.setForeground(new Color(229, 137, 0));
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 15));
        noteLabel = new JLabel("These settings let you configure Postman Integration.");
        encodingLabel = new JLabel("Encoding:");
        encodingTextField = new JTextField(encoding);
        colNameLabel = new JLabel("Collection Name:");
        colNameTextField = new JTextField("");
        folderNameLabel = new JLabel("Folder Name:");
        folderNameTextField = new JTextField("");
        tableModel = new PostmanTableModel(reqCount);
        requestsTable = new JTable(tableModel);

        StringBuilder sb = new StringBuilder("[ ");
        for (int i = 0; i < ExportProcessor.EXCLUDED_HEADERS.length; i++) {
            if (i == ExportProcessor.EXCLUDED_HEADERS.length - 1) {
                sb.append("\"" + ExportProcessor.EXCLUDED_HEADERS[i] + "\"");
            } else {
                sb.append("\"" + ExportProcessor.EXCLUDED_HEADERS[i] + "\", ");
            }
        }
        sb.append(" ]");
        headerNoteLabel = new JLabel("The following request headers will not be exported. " + sb.toString());

        // first column size should be fixed.
        requestsTable.getColumnModel().getColumn(PostmanTableModel.FOLDER_NAME_INDEX).setPreferredWidth(75);
        requestsTable.getColumnModel().getColumn(PostmanTableModel.NAME_COLUMN_INDEX).setPreferredWidth(75);
        requestsTable.getColumnModel().getColumn(PostmanTableModel.METHOD_COLUMN_INDEX).setPreferredWidth(75);
        requestsTable.getColumnModel().getColumn(PostmanTableModel.URL_COLUMN_INDEX).setPreferredWidth(REQ_PANE_WIDTH - 75 - 75 - 75);

        // Generating ScrollPane for Requests Table
        reqScrollPane = new JScrollPane(requestsTable);
        reqScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Setup triangle label for Requests ScrollPane resize.
        triangleLabel = new JLabel("▶");
        triangleLabel.setForeground(new Color(229, 137, 0));
        triangleLabel.setFont(new Font("Dialog", Font.BOLD, 18));

        // Setup Export Button
        exportBtn = new JButton("Export");
        exportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<RequestSource> reqSrcList = new ArrayList<>();
                PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    stdout.println(getURL(reqList.get(i)));
                    RequestSource rs = new RequestSource(reqList.get(i),
                            (String) tableModel.getValueAt(i, PostmanTableModel.NAME_COLUMN_INDEX), (String) tableModel.getValueAt(i, PostmanTableModel.FOLDER_NAME_INDEX));
                    reqSrcList.add(rs);
                }
                ExportProcessor expProcessor = new ExportProcessor(reqSrcList, callbacks, getEncoding(), colNameTextField.getText());

                try {
                    expProcessor.process();
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    pw.flush();
                    String stackTrace = sw.toString();
                    // obtain error streams
                    PrintWriter stderr = new PrintWriter(callbacks.getStderr(), true);
                    stderr.println(stackTrace);
                    callbacks.issueAlert(
                            "Some error happened. Please check Burp Extensions Errors tab. Message:" + ex.getMessage());
                }

                // Update encoding
                encoding = getEncoding();
            }
        });

        autoSetFolderNameBtn = new JButton("Auto Set");
        autoSetFolderNameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < reqCount; i++) {
                    IHttpRequestResponse reqRes = reqList.get(i);
                    try {
                        String url = getURL(reqRes);
                        tableModel.setValueAt(getParentPath(url), i, PostmanTableModel.FOLDER_NAME_INDEX);
                    } catch (MalformedURLException malformed) {
                        continue;
                    }
                }
            }
        });
        // Set up Folder Name Set Button
        setFolderNameBtn = new JButton("Set All");
        setFolderNameBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < reqCount; i++)
                    tableModel.setValueAt(folderNameTextField.getText(), i, PostmanTableModel.FOLDER_NAME_INDEX);
            }
        });

        // Set up Remove Duplicate Name Button
        removeDuplicateURLBtn = new JButton("Remove Duplicate URLs");
        removeDuplicateURLBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> tmpUrl = new ArrayList<String>();
                ArrayList<Integer> duplicateUrl = new ArrayList<Integer>();
                int total = tableModel.getRowCount();
                for (int i = 0; i < total; i++) {
                    String currentUrl = requestsTable.getValueAt(i, PostmanTableModel.URL_COLUMN_INDEX).toString();
                    if (!tmpUrl.contains(currentUrl)) {
                        tmpUrl.add(currentUrl);
                    } else {
                        duplicateUrl.add(i);
                    }
                }
                while (duplicateUrl.size() > 0) {
                    tableModel.removeRow(duplicateUrl.get(0));
                    duplicateUrl.remove(0);
                }
                tmpUrl.clear();
            }
        });

        // set location and size
        titleLabel.setBounds(10, 10, 284, 23);
        noteLabel.setBounds(14, 40, 500, 15);
        encodingLabel.setBounds(15, 65, 69, 23);
        encodingTextField.setBounds(140, 62, 104, 28);
        colNameLabel.setBounds(15, 98, 112, 23);
        colNameTextField.setBounds(139, 95, 179, 28);
        folderNameLabel.setBounds(15, 129, 112, 23);
        folderNameTextField.setBounds(138, 129, 179, 28);
        autoSetFolderNameBtn.setBounds(329, 129, 91, 28);
        setFolderNameBtn.setBounds(432, 129, 91, 28);
        removeDuplicateURLBtn.setBounds(535, 129, 150, 28);
        reqScrollPane.setLocation(REQ_PANE_X, REQ_PANE_Y);
        reqScrollPane.setSize(REQ_PANE_WIDTH, REQ_PANE_HEIGHT);
        triangleLabel.setBounds(REQ_PANE_X + REQ_PANE_WIDTH + 5, REQ_PANE_Y + 70, 20, 110);
        exportBtn.setBounds(15, 430, 91, 28);
        headerNoteLabel.setBounds(14, 462, 796, 28);

        // add to pane
        mainPanel.add(titleLabel);
        mainPanel.add(noteLabel);
        getContentPane().add(encodingLabel);
        getContentPane().add(encodingTextField);
        getContentPane().add(colNameLabel);
        getContentPane().add(colNameTextField);
        mainPanel.add(reqScrollPane);
        mainPanel.add(triangleLabel);
        mainPanel.add(folderNameLabel);
        mainPanel.add(folderNameTextField);
        mainPanel.add(autoSetFolderNameBtn);
        mainPanel.add(setFolderNameBtn);
        mainPanel.add(removeDuplicateURLBtn);
        mainPanel.add(exportBtn);
        mainPanel.add(headerNoteLabel);
        getContentPane().add(mainPanel);


    }

    public PostmanTableModel getTableModel() {
        return this.tableModel;
    }

    public String getEncoding() {
        try {
            "dummy".getBytes(encodingTextField.getText());
            return encodingTextField.getText();
        } catch (UnsupportedEncodingException e) {
            return System.getProperty("file.encoding");
        }
    }

    public void setRequest(List<IHttpRequestResponse> reqList, IBurpExtenderCallbacks callbacks) {
        this.reqList = reqList;
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        for (int i = 0; i < reqList.size(); i++) {
            IHttpRequestResponse reqRes = reqList.get(i);
            IRequestInfo iReqInfo = helpers.analyzeRequest(reqRes.getRequest());
            String name = String.format("%s", getFileName(getURL(reqRes)));
            String method = iReqInfo.getMethod();
            String url = getURL(reqRes);

            /* set value on table */
            this.tableModel.setValueAt(name, i, PostmanTableModel.NAME_COLUMN_INDEX);
            this.tableModel.setValueAt(method, i, PostmanTableModel.METHOD_COLUMN_INDEX);
            this.tableModel.setValueAt(url, i, PostmanTableModel.URL_COLUMN_INDEX);
            // this.tableModel.setValueAt(true, i,
            // PostmanTableModel.ENABLED_COLUMN_INDEX);
        }
    }

    private String getURL(IHttpRequestResponse requestResponse) {
        IHttpService iHS = requestResponse.getHttpService();
        String port = Integer.toString(iHS.getPort());
        String urlPort;
        if (port.equals("80") || port.equals("443"))
            urlPort = "";
        else
            urlPort = ":" + port;

        String url;
        String uri = new String(requestResponse.getRequest()).split("\n")[0].split(" ")[1];
        if (uri.startsWith("http"))
            url = uri;
        else
            url = iHS.getProtocol() + "://" + iHS.getHost() + urlPort
                    + new String(requestResponse.getRequest()).split("\n")[0].split(" ")[1];

        return url;
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        // if resizeFlg is true and X of mouse pointer gt 150 and some space is
        // rest on right side, resize is permitted.
        if (resizeFlg && e.getX() > 150 && e.getX() < this.getWidth() - MINIMUM_SPACE_OF_REQPANE_RIGHTSIDE) {

            // change mouse cursor
            e.getComponent().setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));

            // reqScrollPane is resized.
            reqScrollPane.setSize(new Dimension(e.getX() - REQ_PANE_X, REQ_PANE_HEIGHT));
            reqScrollPane.repaint();
            reqScrollPane.revalidate();

            // triangleLabel is also resized.
            triangleLabel.setBounds(e.getX() + 5, REQ_PANE_Y, 20, 110);
            triangleLabel.repaint();
            triangleLabel.revalidate();

            // Repaint
            this.repaint();
            this.revalidate();
        }
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int w = REQ_PANE_X + reqScrollPane.getWidth();

        // change cursor of mouse pointer
        e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        // if mouse pointer is on resize area right to pane, cursor is
        // changed.
        if ((x > w && x < w + 10) && (y > REQ_PANE_Y && y < REQ_PANE_Y + REQ_PANE_HEIGHT)) {
            e.getComponent().setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
            // if pointer is on resize area, this flag is set to true. This flag
            // is referred from mouseDragged.
            resizeFlg = true;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // if mouse pointer exits from this pane, cursor is changed to default
        // one.
        e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // if mouse is released, cursor is changed to defalut one and resize
        // flag should be always set to false.
        e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        resizeFlg = false;
    }
}