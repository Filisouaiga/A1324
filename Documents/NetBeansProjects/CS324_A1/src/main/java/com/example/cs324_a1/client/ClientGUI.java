/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.client;

import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.jobtype.JobType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Client GUI: enter or load job data, submit jobs to the coordinator through
// Java RMI and watch several tasks run at the same time.
// Several clients can run at once: each window is its own Java process.
public final class ClientGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // Up to this many of this client's jobs are in progress at once; more wait in the queue
    private static final int MAX_CONCURRENT_TASKS = 8;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Color DONE_COLOUR = new Color(0, 128, 0);
    private static final Color FAILED_COLOUR = new Color(190, 0, 0);
    private static final Color RUNNING_COLOUR = new Color(0, 90, 180);
    private static final Color WARNING_COLOUR = new Color(170, 100, 0);

    private final String clientId = "Client-" + ProcessHandle.current().pid();

    // Remote calls run on these background threads, never on the Swing event thread,
    // so the window stays responsive while jobs are running.
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_TASKS,
            Thread.ofPlatform().name("client-task-", 1).daemon(true).factory());

    private final ScheduledExecutorService statusExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("client-status").daemon(true).factory());

    // Replaced on the event thread when the user connects, read by task threads: volatile
    private volatile JobClient jobClient;

    private final AtomicInteger nextTaskNumber = new AtomicInteger();

    // Swing components and the task table are only used on the event thread
    private final JTextField hostField = new JTextField(10);
    private final JTextField portField = new JTextField(5);
    private final JLabel clusterLabel = new JLabel("Connecting...");
    private final JComboBox<JobType> jobTypeBox = new JComboBox<>(JobType.values());
    private final JLabel jobHelpLabel = new JLabel();
    private final CardLayout inputCards = new CardLayout();
    private final JPanel inputPanel = new JPanel(inputCards);
    private final JTextArea numbersArea = new JTextArea(4, 40);
    private final JLabel numbersCountLabel = new JLabel("0 numbers");
    private final JTextField startField = new JTextField("1", 10);
    private final JTextField endField = new JTextField("1000", 10);
    private final JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final TaskTableModel taskModel = new TaskTableModel();
    private final JTable taskTable = new JTable(taskModel);
    private final JLabel taskSummaryLabel = new JLabel();
    private final JTextArea detailsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JSplitPane taskSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    public ClientGUI(String bootstrapHost, int bootstrapPort) {

        super("CS324 Job Client - " + "Client-" + ProcessHandle.current().pid());

        hostField.setText(bootstrapHost);
        portField.setText(String.valueOf(bootstrapPort));

        buildWindow();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                taskSplit.setDividerLocation(0.42);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });

        // Fit smaller laptop screens (the usable area excludes the taskbar)
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1150, screen.width), Math.min(820, screen.height));
        setMinimumSize(new Dimension(900, 600));
        setLocationByPlatform(true);

        log("Client " + clientId + " started.");
        connect();

        statusExecutor.scheduleWithFixedDelay(this::refreshStatus, 3, 3, TimeUnit.SECONDS);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void buildWindow() {

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        root.add(buildConnectionBar(), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(8, 8));
        centre.add(buildJobPanel(), BorderLayout.NORTH);

        taskSplit.setTopComponent(buildTaskPanel());
        taskSplit.setBottomComponent(buildBottomTabs());
        taskSplit.setResizeWeight(0.5);
        taskSplit.setBorder(null);
        centre.add(taskSplit, BorderLayout.CENTER);

        root.add(centre, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildConnectionBar() {

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fields.add(new JLabel("Bootstrap host:"));
        fields.add(hostField);
        fields.add(new JLabel("Port:"));
        fields.add(portField);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        fields.add(connectButton);

        clusterLabel.setFont(clusterLabel.getFont().deriveFont(Font.BOLD));
        clusterLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JPanel bar = new JPanel(new BorderLayout());
        bar.add(fields, BorderLayout.WEST);
        bar.add(clusterLabel, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildJobPanel() {

        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        typeRow.add(new JLabel("Job type:"));
        typeRow.add(jobTypeBox);
        jobHelpLabel.setForeground(Color.DARK_GRAY);
        typeRow.add(jobHelpLabel);
        jobTypeBox.addActionListener(e -> showInputForSelectedJob());

        inputPanel.add(buildNumbersCard(), JobType.MAX.name());
        inputPanel.add(buildRangeCard(), JobType.PRIMESUM.name());

        JPanel submitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        submitRow.add(new JLabel("Copies:"));
        copiesSpinner.setToolTipText("Submit this many copies of the job at the same time");
        submitRow.add(copiesSpinner);

        JButton submitButton = new JButton("Submit Job");
        submitButton.setFont(submitButton.getFont().deriveFont(Font.BOLD));
        submitButton.addActionListener(e -> submitFromForm());
        submitRow.add(submitButton);

        JButton batchButton = new JButton("Submit Batch CSV...");
        batchButton.setToolTipText("One job per row, e.g.  MAX,4,19,7  or  PRIMESUM,1,1000");
        batchButton.addActionListener(e -> submitBatchCsv());
        submitRow.add(batchButton);

        JButton newClientButton = new JButton("Open Another Client");
        newClientButton.setToolTipText("Start another client process to test multiple clients");
        newClientButton.addActionListener(e -> launchAnotherClient());
        submitRow.add(newClientButton);

        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("New job"),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        panel.add(typeRow, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(submitRow, BorderLayout.SOUTH);

        showInputForSelectedJob();
        return panel;
    }

    private JPanel buildNumbersCard() {

        numbersArea.setLineWrap(true);
        numbersArea.setWrapStyleWord(true);
        numbersArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        numbersArea.setText("4, 19, 7, 88, 23, 61, 2, 97, 45, 13");

        // Recount after typing pauses rather than on every key press (the list can be large)
        Timer recount = new Timer(300, e -> updateNumberCount());
        recount.setRepeats(false);
        numbersArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                recount.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                recount.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                recount.restart();
            }
        });
        updateNumberCount();

        numbersArea.setToolTipText("Whole numbers separated by commas, spaces or new lines");

        JButton loadButton = new JButton("Load CSV...");
        loadButton.addActionListener(e -> loadCsvIntoForm());

        JButton randomButton = new JButton("Random...");
        randomButton.addActionListener(e -> generateRandomNumbers());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> numbersArea.setText(""));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(loadButton);
        buttons.add(randomButton);
        buttons.add(clearButton);
        buttons.add(numbersCountLabel);

        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.add(new JScrollPane(numbersArea), BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRangeCard() {

        JButton loadButton = new JButton("Load CSV...");
        loadButton.setToolTipText("Uses the first two numbers in the file as start and end");
        loadButton.addActionListener(e -> loadCsvIntoForm());

        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        fields.add(new JLabel("Start:"));
        fields.add(startField);
        fields.add(new JLabel("End:"));
        fields.add(endField);
        fields.add(loadButton);

        JLabel hint = new JLabel("The coordinator splits the range evenly, e.g. 1-1000 on 4 workers: 1-250, 251-500, 501-750, 751-1000.");
        hint.setForeground(Color.DARK_GRAY);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 2));
        top.add(fields);
        top.add(hint);

        // Keep the fields at the top; the card is as tall as the numbers card
        JPanel card = new JPanel(new BorderLayout());
        card.add(top, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildTaskPanel() {

        taskTable.setRowHeight(22);
        taskTable.setFillsViewportHeight(true);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.getColumnModel().getColumn(TaskTableModel.STATUS_COLUMN).setCellRenderer(new StatusRenderer());

        int[] widths = {40, 190, 330, 130, 110, 150, 80};
        for (int i = 0; i < widths.length; i++) {
            taskTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedTaskDetails();
            }
        });

        taskModel.addTableModelListener(e -> {
            taskSummaryLabel.setText(taskModel.summary());
            showSelectedTaskDetails();
        });
        taskSummaryLabel.setText(taskModel.summary());

        JButton clearButton = new JButton("Clear Finished");
        clearButton.addActionListener(e -> taskModel.removeFinished());

        JPanel header = new JPanel(new BorderLayout());
        header.add(taskSummaryLabel, BorderLayout.WEST);
        header.add(clearButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Tasks"));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(taskTable), BorderLayout.CENTER);
        return panel;
    }

    private JTabbedPane buildBottomTabs() {

        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

        detailsArea.setEditable(false);
        detailsArea.setFont(mono);
        detailsArea.setText("Select a task to see how it was split across the workers.");

        logArea.setEditable(false);
        logArea.setFont(mono);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Task Details", new JScrollPane(detailsArea));
        tabs.addTab("Client Log", new JScrollPane(logArea));
        tabs.setPreferredSize(new Dimension(800, 260));
        return tabs;
    }

    private void showInputForSelectedJob() {

        JobType type = (JobType) jobTypeBox.getSelectedItem();

        inputCards.show(inputPanel, type == JobType.PRIMESUM ? JobType.PRIMESUM.name() : JobType.MAX.name());

        switch (type) {
            case MAX:
                jobHelpLabel.setText("Finds the largest number in the list.");
                break;
            case PRIMESUM:
                jobHelpLabel.setText("Adds up every prime number from start to end (inclusive).");
                break;
            default:
                jobHelpLabel.setText("Counts how many numbers in the list are prime.");
        }
    }

    // ------------------------------------------------------------------
    // Connection and cluster status
    // ------------------------------------------------------------------

    private void connect() {

        String host = hostField.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showError("The Bootstrap port must be a number, e.g. 1099.");
            return;
        }

        if (host.isEmpty()) {
            showError("Enter the Bootstrap host, e.g. localhost.");
            return;
        }

        jobClient = new JobClient(host, port, clientId);
        clusterLabel.setForeground(Color.DARK_GRAY);
        clusterLabel.setText("Connecting to " + host + ":" + port + "...");
        log("Using Bootstrap Node at " + host + ":" + port + ".");

        statusExecutor.execute(this::refreshStatus);
    }

    // Runs on the status thread
    private void refreshStatus() {

        JobClient client = jobClient;

        if (client == null) {
            return;
        }

        try {
            JobClient.ClusterStatus status = client.fetchStatus();

            onEdt(() -> {
                clusterLabel.setForeground(status.hasCoordinator() ? DONE_COLOUR : WARNING_COLOUR);
                clusterLabel.setText(status.hasCoordinator()
                        ? status.toString()
                        : status + " - an election is requested when a job is submitted");
            });

        } catch (JobClient.JobFailedException e) {
            onEdt(() -> {
                clusterLabel.setForeground(FAILED_COLOUR);
                clusterLabel.setText(e.getMessage());
            });
        }
    }

    // ------------------------------------------------------------------
    // Job input
    // ------------------------------------------------------------------

    private void updateNumberCount() {

        CsvLoader.NumberData data = CsvLoader.parseNumbers(numbersArea.getText());
        String text = String.format("%,d numbers", data.getNumbers().size());

        if (data.getSkippedCount() > 0) {
            text += "  -  not whole numbers: " + data.getSkippedSamples();
            numbersCountLabel.setForeground(FAILED_COLOUR);
        } else {
            text += "  (separate with commas, spaces or new lines)";
            numbersCountLabel.setForeground(Color.DARK_GRAY);
        }

        numbersCountLabel.setText(text);
    }

    private void loadCsvIntoForm() {

        File file = chooseCsvFile("Load numbers from CSV");

        if (file == null) {
            return;
        }

        JobType type = (JobType) jobTypeBox.getSelectedItem();

        // Read the file on a background thread; update the form on the event thread in done()
        new SwingWorker<CsvLoader.NumberData, Void>() {

            @Override
            protected CsvLoader.NumberData doInBackground() throws Exception {
                return CsvLoader.readNumbers(file.toPath());
            }

            @Override
            protected void done() {

                CsvLoader.NumberData data;

                try {
                    data = get();
                } catch (Exception e) {
                    showError("Could not read " + file.getName() + ": " + JobClient.rootMessage(e));
                    return;
                }

                List<Integer> numbers = data.getNumbers();
                String skipped = data.getSkippedCount() == 0 ? ""
                        : " (skipped " + data.getSkippedCount() + " value(s) that are not whole numbers, e.g. "
                        + data.getSkippedSamples() + ")";

                if (numbers.isEmpty()) {
                    showError(file.getName() + " does not contain any whole numbers." + skipped);
                    return;
                }

                if (type == JobType.PRIMESUM) {

                    if (numbers.size() < 2) {
                        showError("PRIMESUM needs two numbers (start and end) but " + file.getName() + " has one.");
                        return;
                    }

                    startField.setText(String.valueOf(numbers.get(0)));
                    endField.setText(String.valueOf(numbers.get(1)));
                    log("Loaded start=" + numbers.get(0) + ", end=" + numbers.get(1) + " from " + file.getName() + skipped);

                } else {
                    numbersArea.setText(formatNumbers(numbers));
                    numbersArea.setCaretPosition(0);
                    log(String.format("Loaded %,d numbers from %s%s", numbers.size(), file.getName(), skipped));
                }
            }
        }.execute();
    }

    private void generateRandomNumbers() {

        JTextField countField = new JTextField("1000");
        JTextField minField = new JTextField("1");
        JTextField maxField = new JTextField("100000");

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.add(new JLabel("How many numbers:"));
        form.add(countField);
        form.add(new JLabel("Smallest value:"));
        form.add(minField);
        form.add(new JLabel("Largest value:"));
        form.add(maxField);

        int choice = JOptionPane.showConfirmDialog(this, form, "Generate random numbers",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        Integer count = parseWholeNumber(countField.getText(), "How many numbers");
        Integer min = parseWholeNumber(minField.getText(), "Smallest value");
        Integer max = parseWholeNumber(maxField.getText(), "Largest value");

        if (count == null || min == null || max == null) {
            return;
        }

        if (count < 1 || count > 2_000_000) {
            showError("Choose between 1 and 2,000,000 numbers.");
            return;
        }

        if (min > max) {
            showError("The smallest value cannot be larger than the largest value.");
            return;
        }

        List<Integer> numbers = new ArrayList<>(count);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            numbers.add((int) random.nextLong(min, (long) max + 1));
        }

        numbersArea.setText(formatNumbers(numbers));
        numbersArea.setCaretPosition(0);
        log(String.format("Generated %,d random numbers between %d and %d.", count, min, max));
    }

    // ------------------------------------------------------------------
    // Submitting and running tasks
    // ------------------------------------------------------------------

    private void submitFromForm() {

        JobType type = (JobType) jobTypeBox.getSelectedItem();
        int copies = (Integer) copiesSpinner.getValue();
        List<JobRequest> requests = new ArrayList<>();

        if (type == JobType.PRIMESUM) {

            Integer start = parseWholeNumber(startField.getText(), "Start");
            Integer end = parseWholeNumber(endField.getText(), "End");

            if (start == null || end == null) {
                return;
            }

            if (start > end) {
                showError("Start (" + start + ") cannot be greater than End (" + end + ").");
                return;
            }

            for (int i = 0; i < copies; i++) {
                requests.add(new JobRequest(type, start, end, clientId));
            }

        } else {

            CsvLoader.NumberData data = CsvLoader.parseNumbers(numbersArea.getText());

            if (data.getSkippedCount() > 0) {
                showError("These values are not whole numbers: " + data.getSkippedSamples());
                return;
            }

            if (data.getNumbers().isEmpty()) {
                showError(type + " needs at least one number. Type some in, load a CSV file or use Random...");
                return;
            }

            for (int i = 0; i < copies; i++) {
                requests.add(new JobRequest(type, data.getNumbers(), clientId));
            }
        }

        submitTasks(requests);
    }

    private void submitBatchCsv() {

        File file = chooseCsvFile("Submit a batch of jobs from CSV");

        if (file == null) {
            return;
        }

        CsvLoader.BatchData batch;

        try {
            batch = CsvLoader.readBatch(file.toPath(), clientId);
        } catch (Exception e) {
            showError("Could not read " + file.getName() + ": " + JobClient.rootMessage(e));
            return;
        }

        for (String error : batch.getErrors()) {
            log(file.getName() + " - " + error);
        }

        if (batch.getJobs().isEmpty()) {
            showError(file.getName() + " contains no valid job rows. Expected rows like  MAX,4,19,7  or  PRIMESUM,1,1000.");
            return;
        }

        String message = "Submit " + batch.getJobs().size() + " job(s) from " + file.getName() + " at the same time?";

        if (!batch.getErrors().isEmpty()) {
            message += "\n" + batch.getErrors().size() + " row(s) were skipped - see the Client Log tab.";
        }

        if (JOptionPane.showConfirmDialog(this, message, "Submit batch", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            submitTasks(batch.getJobs());
        }
    }

    // Called on the event thread
    private void submitTasks(List<JobRequest> requests) {

        JobClient client = jobClient;

        for (JobRequest request : requests) {

            TaskRow row = new TaskRow(nextTaskNumber.incrementAndGet(), request);
            taskModel.add(row);
            log("Task #" + row.number + " queued: " + request.describe());

            taskExecutor.execute(() -> runTask(client, row));
        }
    }

    // Runs on a client-task thread. The row is only changed through onEdt(), so the
    // table is never modified by two threads at once (thread confinement).
    private void runTask(JobClient client, TaskRow row) {

        String thread = Thread.currentThread().getName();
        onEdt(() -> taskModel.update(row, TaskState.RUNNING, "Finding coordinator..."));
        log("Task #" + row.number + " started on client thread " + thread + ".");

        try {
            JobResult result = client.submit(row.request, message -> {
                onEdt(() -> taskModel.update(row, TaskState.RUNNING, message));
                log("Task #" + row.number + ": " + message);
            });

            onEdt(() -> taskModel.complete(row, result));
            log("Task #" + row.number + " done: " + row.request.describe() + " = " + result.getResult()
                    + " (coordinator Worker " + result.getWorkerId() + ", " + result.getPartialResults().size() + " partial results)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onEdt(() -> taskModel.fail(row, "Cancelled"));

        } catch (Exception e) {
            String reason = JobClient.rootMessage(e);
            onEdt(() -> taskModel.fail(row, reason));
            log("Task #" + row.number + " FAILED: " + reason);
        }
    }

    private void launchAnotherClient() {

        try {
            String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

            ProcessBuilder builder = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                    ClientGUI.class.getName(), hostField.getText().trim(), portField.getText().trim());
            builder.inheritIO();

            Process process = builder.start();
            log("Started another client process (PID " + process.pid() + ").");

        } catch (Exception e) {
            showError("Could not start another client: " + JobClient.rootMessage(e));
        }
    }

    // ------------------------------------------------------------------
    // Task details
    // ------------------------------------------------------------------

    private void showSelectedTaskDetails() {

        int viewRow = taskTable.getSelectedRow();

        if (viewRow < 0) {
            return;
        }

        TaskRow row = taskModel.rowAt(taskTable.convertRowIndexToModel(viewRow));
        String text = describeTask(row);

        if (!text.equals(detailsArea.getText())) {
            detailsArea.setText(text);
            detailsArea.setCaretPosition(0);
        }
    }

    private String describeTask(TaskRow row) {

        JobRequest request = row.request;
        StringBuilder text = new StringBuilder();

        text.append("Task #").append(row.number).append("  ").append(request.describe());

        if (row.state == TaskState.DONE) {

            JobResult result = row.result;
            text.append(" = ").append(result.getResult()).append('\n');
            text.append("Coordinator Worker ").append(result.getWorkerId()).append(" (term ").append(result.getCoordinatorTerm())
                    .append(")  |  ").append(row.finishedAt - row.submittedAt).append(" ms round trip, ")
                    .append(result.getDurationMillis()).append(" ms on the coordinator\n");
            text.append("Input: ").append(describeInput(request)).append('\n');
            text.append("Client ").append(request.getClientId()).append("  |  Job ID ").append(request.getJobId()).append("\n\n");

            text.append("Partial results, combined with ")
                    .append(result.getJobType() == JobType.MAX ? "MAX" : "SUM").append(":\n");
            text.append(String.format("  %-7s %-34s %16s  %-22s %8s%n", "Worker", "Portion", "Result", "Thread", "Time"));

            for (JobResult part : result.getPartialResults()) {
                text.append(String.format("  %-7s %-34s %16d  %-22s %6d ms%n", "W" + part.getWorkerId(),
                        part.getDetail(), part.getResult(), part.getThreadName(), part.getDurationMillis()));
            }

            text.append(String.format("  %-42s %16d%n", "Combined result", result.getResult()));

        } else {
            text.append('\n');
            text.append("Status: ").append(row.statusText).append('\n');
            text.append("Input: ").append(describeInput(request)).append('\n');
            text.append("Client ").append(request.getClientId()).append("  |  Job ID ").append(request.getJobId()).append('\n');
        }

        return text.toString();
    }

    private static String describeInput(JobRequest request) {

        if (request.getJobType() == JobType.PRIMESUM) {
            return "primes from " + request.getStart() + " to " + request.getEnd();
        }

        List<Integer> numbers = request.getNumbers();
        String preview = numbers.subList(0, Math.min(10, numbers.size())).toString();

        if (numbers.size() > 10) {
            preview = preview.substring(0, preview.length() - 1) + ", ...]";
        }

        return String.format("%,d numbers %s", numbers.size(), preview);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private File chooseCsvFile(String title) {

        File samples = new File(System.getProperty("user.dir"), "samples");
        JFileChooser chooser = new JFileChooser(samples.isDirectory() ? samples : new File(System.getProperty("user.dir")));
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("CSV or text files (*.csv, *.txt)", "csv", "txt"));

        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private Integer parseWholeNumber(String text, String fieldName) {

        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            showError(fieldName + " must be a whole number between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE + ".");
            return null;
        }
    }

    // 20 numbers per line keeps the text area fast even for very large lists
    private static String formatNumbers(List<Integer> numbers) {

        StringBuilder text = new StringBuilder(numbers.size() * 8);

        for (int i = 0; i < numbers.size(); i++) {

            if (i > 0) {
                text.append(i % 20 == 0 ? ",\n" : ", ");
            }

            text.append(numbers.get(i));
        }

        return text.toString();
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(this, message, "CS324 Job Client", JOptionPane.ERROR_MESSAGE);
    }

    // Safe to call from any thread
    private void log(String message) {

        String line = LocalTime.now().format(TIME) + "  " + message + "\n";

        onEdt(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static void onEdt(Runnable action) {

        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private void closeWindow() {

        int running = taskModel.countActive();

        if (running > 0 && JOptionPane.showConfirmDialog(this,
                running + " task(s) are still running. Close the client anyway?", "Close client",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        taskExecutor.shutdownNow();
        statusExecutor.shutdownNow();
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------
    // Task table
    // ------------------------------------------------------------------

    private enum TaskState {
        QUEUED, RUNNING, DONE, FAILED
    }

    // Mutable fields are only changed on the event thread (via TaskTableModel)
    private static final class TaskRow {

        final int number;
        final JobRequest request;
        final long submittedAt = System.currentTimeMillis();

        TaskState state = TaskState.QUEUED;
        String statusText = "Queued";
        JobResult result;
        long finishedAt;

        TaskRow(int number, JobRequest request) {
            this.number = number;
            this.request = request;
        }
    }

    private static final class TaskTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        static final int STATUS_COLUMN = 2;

        private static final String[] COLUMNS = {"#", "Job", "Status", "Result", "Coordinator", "Workers", "Time"};

        private final List<TaskRow> rows = new ArrayList<>();

        void add(TaskRow row) {
            rows.add(row);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void update(TaskRow row, TaskState state, String statusText) {

            if (row.state == TaskState.DONE || row.state == TaskState.FAILED) {
                return;
            }

            row.state = state;
            row.statusText = statusText;
            fireRowChanged(row);
        }

        void complete(TaskRow row, JobResult result) {
            row.state = TaskState.DONE;
            row.statusText = "Done";
            row.result = result;
            row.finishedAt = System.currentTimeMillis();
            fireRowChanged(row);
        }

        void fail(TaskRow row, String reason) {
            row.state = TaskState.FAILED;
            row.statusText = "Failed: " + reason;
            row.finishedAt = System.currentTimeMillis();
            fireRowChanged(row);
        }

        void removeFinished() {
            rows.removeIf(row -> row.state == TaskState.DONE || row.state == TaskState.FAILED);
            fireTableDataChanged();
        }

        TaskRow rowAt(int index) {
            return rows.get(index);
        }

        int countActive() {
            return count(TaskState.QUEUED) + count(TaskState.RUNNING);
        }

        String summary() {
            return "Queued: " + count(TaskState.QUEUED) + "    Running: " + count(TaskState.RUNNING)
                    + "    Done: " + count(TaskState.DONE) + "    Failed: " + count(TaskState.FAILED);
        }

        private int count(TaskState state) {

            int total = 0;

            for (TaskRow row : rows) {
                if (row.state == state) {
                    total++;
                }
            }

            return total;
        }

        private void fireRowChanged(TaskRow row) {

            int index = rows.indexOf(row);

            if (index >= 0) {
                fireTableRowsUpdated(index, index);
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int column) {

            TaskRow row = rows.get(rowIndex);
            JobResult result = row.result;

            switch (column) {
                case 0:
                    return row.number;
                case 1:
                    return row.request.describe();
                case 2:
                    return row.statusText;
                case 3:
                    return result == null ? "" : String.format("%,d", result.getResult());
                case 4:
                    return result == null ? "" : "W" + result.getWorkerId() + " (term " + result.getCoordinatorTerm() + ")";
                case 5:
                    return result == null ? "" : workersUsed(result);
                default:
                    return row.finishedAt == 0 ? "" : String.format("%,d ms", row.finishedAt - row.submittedAt);
            }
        }

        private static String workersUsed(JobResult result) {

            TreeSet<Integer> ids = new TreeSet<>();

            for (JobResult part : result.getPartialResults()) {
                ids.add(part.getWorkerId());
            }

            StringBuilder text = new StringBuilder();

            for (int id : ids) {
                text.append(text.length() == 0 ? "" : ", ").append("W").append(id);
            }

            return text.toString();
        }
    }

    private final class StatusRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {

                TaskState state = taskModel.rowAt(table.convertRowIndexToModel(row)).state;

                switch (state) {
                    case DONE:
                        setForeground(DONE_COLOUR);
                        break;
                    case FAILED:
                        setForeground(FAILED_COLOUR);
                        break;
                    case RUNNING:
                        setForeground(RUNNING_COLOUR);
                        break;
                    default:
                        setForeground(Color.GRAY);
                }
            }

            setToolTipText(String.valueOf(value));
            return this;
        }
    }

    public static void main(String[] args) {

        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to the default look and feel
            }

            new ClientGUI(host, port).setVisible(true);
        });
    }
}
