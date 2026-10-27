import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class RecursionVisualizer extends JFrame {

    enum StepType { CALL, BASE_CASE, RETURN }

    static class StackFrame {
        String name;
        int param;
        String status;
        Integer returnVal;

        StackFrame(String name, int param, String status) {
            this.name = name;
            this.param = param;
            this.status = status;
        }
    }

    static class TreeNode {
        String label;
        int n;
        int result = -1;
        int x, y;
        StepType type;
        TreeNode left, right;
        int callId;

        TreeNode(String label, int n, StepType type, int callId) {
            this.label = label;
            this.n = n;
            this.type = type;
            this.callId = callId;
        }
    }

    static class Step {
        StepType type;
        String label;
        int nodeParam;
        int activeCall;
        List<StackFrame> stackSnapshot;
        TreeNode root;

        Step(StepType type, String label, int nodeParam,
             List<StackFrame> stack, TreeNode root, int activeCall) {
            this.type = type;
            this.label = label;
            this.nodeParam = nodeParam;
            this.activeCall = activeCall;
            this.root = root;

            stackSnapshot = new ArrayList<>();
            for (StackFrame f : stack) {
                StackFrame copy = new StackFrame(
                    f.name, f.param, f.status
                );
                copy.returnVal = f.returnVal;
                stackSnapshot.add(copy);
            }
        }
    }

    private final List<Step> executionSteps = new ArrayList<>();
    private int currentStepIndex = 0;
    private int callCounter = 0;
    private boolean isPlaying = false;
    private Timer playTimer;

    private JComboBox<String> problemSelector;
    private JSlider inputSlider;
    private JLabel inputLabel;
    private JButton playBtn, stepForwardBtn,
                    stepBackwardBtn, resetBtn;
    private JLabel statusLabel, resultLabel;

    private TreeCanvasPanel treePanel;
    private StackPanel stackPanel;
    private SpiralPanel spiralPanel;

    public RecursionVisualizer() {
        setTitle("Recursion Studio");
        setSize(1250, 820);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        generateSteps();
        updateUIState();
    }

    private void initComponents() {
        setLayout(new BorderLayout(6, 6));

        JPanel controls = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 10, 8)
        );
        controls.setBackground(new Color(235, 241, 250));
        controls.setBorder(new EmptyBorder(4, 6, 4, 6));

        problemSelector = new JComboBox<>(new String[]{
            "Fibonacci", "Factorial", "Spiral + Factorial"
        });
        problemSelector.addActionListener(e -> resetExecution());

        inputSlider = new JSlider(1, 7, 5);
        inputSlider.setMajorTickSpacing(1);
        inputSlider.setPaintTicks(true);
        inputSlider.setPaintLabels(true);
        inputSlider.setPreferredSize(new Dimension(155, 45));

        inputLabel = new JLabel("n = 5");
        inputSlider.addChangeListener(e -> {
            inputLabel.setText("n = " + inputSlider.getValue());
            if (!inputSlider.getValueIsAdjusting()) {
                resetExecution();
            }
        });

        stepBackwardBtn = new JButton("<< Back");
        playBtn = new JButton("Play");
        stepForwardBtn = new JButton("Next >>");
        resetBtn = new JButton("Reset");

        stepBackwardBtn.addActionListener(e -> stepBackward());
        stepForwardBtn.addActionListener(e -> stepForward());
        playBtn.addActionListener(this::togglePlay);
        resetBtn.addActionListener(e -> resetExecution());

        controls.add(new JLabel("Algorithm:"));
        controls.add(problemSelector);
        controls.add(new JLabel("Input:"));
        controls.add(inputSlider);
        controls.add(inputLabel);
        controls.add(stepBackwardBtn);
        controls.add(playBtn);
        controls.add(stepForwardBtn);
        controls.add(resetBtn);

        add(controls, BorderLayout.NORTH);

        treePanel = new TreeCanvasPanel();
        stackPanel = new StackPanel();
        spiralPanel = new SpiralPanel();

        JScrollPane stackScroll = new JScrollPane(stackPanel);
        stackScroll.setBorder(null);
        stackScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.add(stackScroll, BorderLayout.CENTER);
        rightPanel.add(spiralPanel, BorderLayout.SOUTH);
        rightPanel.setPreferredSize(new Dimension(350, 500));

        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, treePanel, rightPanel
        );
        split.setResizeWeight(0.70);
        split.setDividerLocation(850);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setBorder(new EmptyBorder(5, 12, 10, 12));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        resultLabel = new JLabel("Final result: --");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        resultLabel.setForeground(new Color(40, 105, 190));

        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(resultLabel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        playTimer = new Timer(650, e -> {
            if (currentStepIndex < executionSteps.size() - 1) {
                stepForward();
            } else {
                stopPlaying();
            }
        });
    }
        private void generateSteps() {
        executionSteps.clear();
        callCounter = 0;
        currentStepIndex = 0;

        int n = inputSlider.getValue();
        String selected = (String) problemSelector.getSelectedItem();
        List<StackFrame> stack = new ArrayList<>();

        TreeNode root;

        if ("Factorial".equals(selected)
                || "Spiral + Factorial".equals(selected)) {
            root = buildFactorial(n, stack);
        } else {
            root = buildFibonacci(n, stack);
        }

        attachTreeToSteps(root);
    }

    private TreeNode buildFactorial(int n, List<StackFrame> stack) {
        int id = ++callCounter;

        TreeNode node = new TreeNode(
            "fact(" + n + ")", n, StepType.CALL, id
        );

        StackFrame frame = new StackFrame(
            "fact", n, "Calling fact(" + n + ")"
        );

        stack.add(0, frame);

        record(
            StepType.CALL,
            "Call fact(" + n + ")",
            n, stack, id
        );

        if (n <= 1) {
            frame.status = "Base case reached";
            frame.returnVal = 1;
            node.type = StepType.BASE_CASE;
            node.result = 1;

            record(
                StepType.BASE_CASE,
                "Base case: fact(" + n + ") = 1",
                n, stack, id
            );

            stack.remove(0);
            return node;
        }

        node.left = buildFactorial(n - 1, stack);

        int result = n * node.left.result;

        node.result = result;
        node.type = StepType.RETURN;

        StackFrame returning = new StackFrame(
            "fact", n,
            n + " x " + node.left.result + " = " + result
        );

        returning.returnVal = result;
        stack.add(0, returning);

        record(
            StepType.RETURN,
            "Return fact(" + n + ") = " + n
                + " x " + node.left.result + " = " + result,
            n, stack, id
        );

        stack.remove(0);
        return node;
    }

    private TreeNode buildFibonacci(int n, List<StackFrame> stack) {
        int id = ++callCounter;

        TreeNode node = new TreeNode(
            "fib(" + n + ")", n, StepType.CALL, id
        );

        StackFrame frame = new StackFrame(
            "fib", n, "Calling fib(" + n + ")"
        );

        stack.add(0, frame);

        record(
            StepType.CALL,
            "Call fib(" + n + ")",
            n, stack, id
        );

        if (n <= 1) {
            frame.status = "Base case reached";
            frame.returnVal = n;
            node.type = StepType.BASE_CASE;
            node.result = n;

            record(
                StepType.BASE_CASE,
                "Base case: fib(" + n + ") = " + n,
                n, stack, id
            );

            stack.remove(0);
            return node;
        }

        node.left = buildFibonacci(n - 1, stack);
        node.right = buildFibonacci(n - 2, stack);

        int result = node.left.result + node.right.result;

        node.result = result;
        node.type = StepType.RETURN;

        StackFrame returning = new StackFrame(
            "fib", n,
            node.left.result + " + "
                + node.right.result + " = " + result
        );

        returning.returnVal = result;
        stack.add(0, returning);

        record(
            StepType.RETURN,
            "Return fib(" + n + ") = "
                + node.left.result + " + "
                + node.right.result + " = " + result,
            n, stack, id
        );

        stack.remove(0);
        return node;
    }

    private void record(
            StepType type,
            String label,
            int n,
            List<StackFrame> stack,
            int activeCall) {

        executionSteps.add(new Step(
            type, label, n, stack, null, activeCall
        ));
    }

    private void attachTreeToSteps(TreeNode root) {
        for (Step step : executionSteps) {
            step.root = root;
        }
    }

    private void stepForward() {
        if (currentStepIndex < executionSteps.size() - 1) {
            currentStepIndex++;
            updateUIState();
        }
    }

    private void stepBackward() {
        if (currentStepIndex > 0) {
            currentStepIndex--;
            updateUIState();
        }
    }

    private void togglePlay(ActionEvent e) {
        if (isPlaying) {
            stopPlaying();
        } else {
            if (currentStepIndex >= executionSteps.size() - 1) {
                currentStepIndex = 0;
                updateUIState();
            }

            isPlaying = true;
            playBtn.setText("Pause");
            playTimer.start();
        }
    }

    private void stopPlaying() {
        isPlaying = false;
        playTimer.stop();
        playBtn.setText("Play");
    }

    private void resetExecution() {
        stopPlaying();
        generateSteps();
        updateUIState();
    }

    private void updateUIState() {
        if (executionSteps.isEmpty()) {
            return;
        }

        Step step = executionSteps.get(currentStepIndex);

        statusLabel.setText(
            "Step " + (currentStepIndex + 1)
                + " / " + executionSteps.size()
                + " | " + step.label
        );

        if (step.type == StepType.BASE_CASE) {
            statusLabel.setForeground(new Color(205, 115, 20));
        } else if (step.type == StepType.RETURN) {
            statusLabel.setForeground(new Color(20, 145, 90));
        } else {
            statusLabel.setForeground(new Color(50, 90, 160));
        }

        int n = inputSlider.getValue();
        String selected = (String) problemSelector.getSelectedItem();

        int result;

        if ("Factorial".equals(selected)
                || "Spiral + Factorial".equals(selected)) {
            result = factorialValue(n);
        } else {
            result = fibonacciValue(n);
        }

        resultLabel.setText("Final result: " + result);

        treePanel.setState(step, selected, n);
        stackPanel.renderStack(step.stackSnapshot);
        spiralPanel.setState(step, n);

        stepBackwardBtn.setEnabled(currentStepIndex > 0);

        stepForwardBtn.setEnabled(
            currentStepIndex < executionSteps.size() - 1
        );
    }

    private int factorialValue(int n) {
        int result = 1;

        for (int i = 2; i <= n; i++) {
            result *= i;
        }

        return result;
    }

    private int fibonacciValue(int n) {
        if (n <= 1) {
            return n;
        }

        int a = 0;
        int b = 1;

        for (int i = 2; i <= n; i++) {
            int c = a + b;
            a = b;
            b = c;
        }

        return b;
    }
        static class TreeCanvasPanel extends JPanel {
        private Step step;
        private String algorithm;
        private int input;

        TreeCanvasPanel() {
            setBackground(new Color(250, 252, 255));
            setBorder(BorderFactory.createTitledBorder(
                "RECURSION MAP - Calls, branches & return values"
            ));
        }

        void setState(Step step, String algorithm, int input) {
            this.step = step;
            this.algorithm = algorithm;
            this.input = input;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (step == null || step.root == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            // Background dot grid
            g2.setColor(new Color(225, 234, 247));

            for (int x = 20; x < width; x += 28) {
                for (int y = 20; y < height; y += 28) {
                    g2.fillOval(x, y, 2, 2);
                }
            }

            // Header
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(new Color(65, 80, 105));

            g2.drawString(
                "Algorithm: " + algorithm + "    Input n = " + input,
                20, 25
            );

            int treeHeight = treeHeight(step.root);

            int verticalSpacing = Math.max(
                42,
                Math.min(
                    78,
                    (height - 100) / Math.max(1, treeHeight)
                )
            );

            layoutTree(
                step.root,
                0,
                30,
                Math.max(1, width - 60),
                verticalSpacing
            );

            drawTree(g2, step.root, step.activeCall);

            // Legend
            int legendY = height - 18;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

            g2.setColor(new Color(65, 125, 220));
            g2.fillOval(20, legendY - 9, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Call", 35, legendY);

            g2.setColor(new Color(245, 175, 75));
            g2.fillOval(90, legendY - 9, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Base case", 105, legendY);

            g2.setColor(new Color(90, 195, 135));
            g2.fillOval(195, legendY - 9, 10, 10);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Returned", 210, legendY);

            g2.dispose();
        }

        private int treeHeight(TreeNode node) {
            if (node == null) {
                return 0;
            }

            return 1 + Math.max(
                treeHeight(node.left),
                treeHeight(node.right)
            );
        }

        private int countLeaves(TreeNode node) {
            if (node == null) {
                return 0;
            }

            if (node.left == null && node.right == null) {
                return 1;
            }

            return countLeaves(node.left)
                + countLeaves(node.right);
        }

        private void layoutTree(
                TreeNode node,
                int depth,
                int startX,
                int availableWidth,
                int verticalSpacing) {

            if (node == null) {
                return;
            }

            node.y = 65 + depth * verticalSpacing;

            int leftLeaves = node.left == null
                ? 0 : countLeaves(node.left);

            int rightLeaves = node.right == null
                ? 0 : countLeaves(node.right);

            int totalLeaves = Math.max(
                1, leftLeaves + rightLeaves
            );

            if (node.left == null && node.right == null) {
                node.x = startX + availableWidth / 2;
                return;
            }

            int leftWidth = node.left == null
                ? 0
                : Math.max(
                    1,
                    availableWidth * leftLeaves / totalLeaves
                );

            int rightWidth = Math.max(
                1, availableWidth - leftWidth
            );

            if (node.left != null) {
                layoutTree(
                    node.left,
                    depth + 1,
                    startX,
                    leftWidth,
                    verticalSpacing
                );
            }

            if (node.right != null) {
                layoutTree(
                    node.right,
                    depth + 1,
                    startX + leftWidth,
                    rightWidth,
                    verticalSpacing
                );
            }

            if (node.left != null && node.right != null) {
                node.x = (
                    node.left.x + node.right.x
                ) / 2;
            } else if (node.left != null) {
                node.x = node.left.x;
            } else {
                node.x = node.right.x;
            }
        }

        private void drawTree(
                Graphics2D g,
                TreeNode node,
                int activeCall) {

            if (node == null) {
                return;
            }

            // Draw left branch
            if (node.left != null) {
                g.setColor(new Color(155, 175, 205));
                g.setStroke(new BasicStroke(1.5f));

                g.drawLine(
                    node.x, node.y + 18,
                    node.left.x, node.left.y - 18
                );

                drawTree(g, node.left, activeCall);
            }

            // Draw right branch
            if (node.right != null) {
                g.setColor(new Color(155, 175, 205));
                g.setStroke(new BasicStroke(1.5f));

                g.drawLine(
                    node.x, node.y + 18,
                    node.right.x, node.right.y - 18
                );

                drawTree(g, node.right, activeCall);
            }

            // Choose node color based on execution state
            Color fill;

            if (node.type == StepType.BASE_CASE) {
                fill = new Color(255, 222, 165);
            } else if (node.type == StepType.RETURN) {
                fill = new Color(190, 238, 210);
            } else {
                fill = new Color(210, 229, 252);
            }

            boolean active = node.callId == activeCall;
            int radius = active ? 22 : 18;

            if (active) {
                fill = new Color(150, 185, 255);
            }

            g.setColor(fill);

            g.fillOval(
                node.x - radius,
                node.y - radius,
                radius * 2,
                radius * 2
            );

            g.setColor(
                active
                    ? new Color(30, 80, 190)
                    : new Color(65, 85, 125)
            );

            g.setStroke(new BasicStroke(active ? 2.5f : 1.2f));

            g.drawOval(
                node.x - radius,
                node.y - radius,
                radius * 2,
                radius * 2
            );

            // Display input and computed result
            g.setFont(new Font("SansSerif", Font.BOLD, 10));

            String label = node.result >= 0
                ? node.n + ":" + node.result
                : Integer.toString(node.n);

            FontMetrics fm = g.getFontMetrics();

            g.setColor(new Color(30, 40, 60));

            g.drawString(
                label,
                node.x - fm.stringWidth(label) / 2,
                node.y + 4
            );
        }
    }
        static class StackPanel extends JPanel {

        StackPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(new Color(244, 247, 252));

            setBorder(BorderFactory.createTitledBorder(
                "LIVE CALL STACK - Top frame first"
            ));
        }

        void renderStack(List<StackFrame> frames) {
            removeAll();

            if (frames.isEmpty()) {
                JLabel empty = new JLabel("Stack empty");

                empty.setBorder(
                    new EmptyBorder(15, 10, 15, 10)
                );

                add(empty);
            } else {
                int count = 0;

                for (StackFrame f : frames) {
                    JPanel card = new JPanel(
                        new BorderLayout(5, 5)
                    );

                    card.setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(
                                count == 0
                                    ? new Color(65, 120, 230)
                                    : new Color(205, 215, 230),
                                1
                            ),
                            new EmptyBorder(7, 10, 7, 10)
                        )
                    );

                    card.setMaximumSize(
                        new Dimension(Integer.MAX_VALUE, 65)
                    );

                    if (count == 0) {
                        card.setBackground(
                            new Color(222, 233, 255)
                        );
                    } else if (f.returnVal != null) {
                        card.setBackground(
                            new Color(218, 244, 227)
                        );
                    } else {
                        card.setBackground(Color.WHITE);
                    }

                    JLabel title = new JLabel(
                        (count == 0 ? "ACTIVE  " : "       ")
                            + f.name + "(" + f.param + ")"
                    );

                    title.setFont(
                        new Font("SansSerif", Font.BOLD, 13)
                    );

                    String detailText;

                    if (f.returnVal != null) {
                        detailText =
                            "Return value: " + f.returnVal;
                    } else {
                        detailText = f.status;
                    }

                    JLabel detail = new JLabel(detailText);

                    detail.setFont(
                        new Font("SansSerif", Font.PLAIN, 11)
                    );

                    card.add(title, BorderLayout.NORTH);
                    card.add(detail, BorderLayout.SOUTH);

                    add(card);
                    add(Box.createRigidArea(
                        new Dimension(0, 5)
                    ));

                    count++;
                }
            }

            revalidate();
            repaint();
        }
    }

    static class SpiralPanel extends JPanel {

        private Step step;
        private int n;

        SpiralPanel() {
            setPreferredSize(new Dimension(300, 235));
            setBackground(new Color(20, 26, 45));

            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                    new Color(90, 110, 150)
                ),
                "RECURSION SPIRAL"
            ));
        }

        void setState(Step step, int n) {
            this.step = step;
            this.n = n;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            int cx = width / 2;
            int cy = height / 2 - 5;

            double previousX = cx;
            double previousY = cy;

            double angle = 0;
            double radius = 0;

            int segments = 200 + n * 50;

            for (int i = 1; i <= segments; i++) {
                angle += 0.12;
                radius += 0.17;

                double x = cx + Math.cos(angle) * radius;
                double y = cy + Math.sin(angle) * radius;

                float hue = (float) i / segments;

                g2.setColor(
                    Color.getHSBColor(hue, 0.8f, 1.0f)
                );

                g2.setStroke(new BasicStroke(2.0f));

                g2.drawLine(
                    (int) previousX,
                    (int) previousY,
                    (int) x,
                    (int) y
                );

                previousX = x;
                previousY = y;
            }

            // Center point
            g2.setColor(Color.WHITE);

            g2.fillOval(
                cx - 4,
                cy - 4,
                8,
                8
            );

            // Spiral information
            g2.setFont(
                new Font("SansSerif", Font.BOLD, 11)
            );

            g2.setColor(new Color(225, 235, 255));

            String state = step == null
                ? "Ready"
                : step.type.toString();

            g2.drawString(
                "n = " + n + "  |  " + state,
                10,
                height - 10
            );

            g2.dispose();
        }
    }
        public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            RecursionVisualizer app = new RecursionVisualizer();
            app.setVisible(true);
        });
    }
}