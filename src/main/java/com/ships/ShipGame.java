package com.ships;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;
import java.util.List;

public final class ShipGame {
    private static final Color NAVY = new Color(10, 31, 48);
    private static final Color PANEL = new Color(17, 48, 66);
    private static final Color SEA = new Color(17, 73, 88);
    private final JFrame frame = new JFrame("Ships");
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final GameStore store;
    private List<ShipPreset> presets;
    private GamePanel gamePanel;
    private GameStore.SavedGame pendingLoad;

    private ShipGame() throws IOException {
        store = new GameStore();
        presets = store.loadPresets();
        if (presets.isEmpty()) {
            presets = ShipPreset.defaults();
            store.savePresets(presets);
        }
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 640));
        frame.setSize(1100, 760);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(root);
        showMenu();
        frame.setVisible(true);
    }

    private void showMenu() {
        JPanel menu = new JPanel(new GridBagLayout());
        menu.setBackground(NAVY);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("SHIPS");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        title.setForeground(new Color(226, 241, 235));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("A tactical top-down naval game");
        subtitle.setForeground(new Color(158, 195, 193));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(38));
        content.add(menuButton("New Game", this::showNewGame));
        content.add(menuButton("Load Game", this::showLoadGames));
        content.add(menuButton("Ship Presets", this::showEditor));
        content.add(Box.createVerticalStrut(8));
        JLabel hint = new JLabel("Mouse to steer  ·  W A S D to fire  ·  Esc to pause");
        hint.setForeground(new Color(158, 195, 193));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(hint);
        menu.add(content);
        showCard(menu, "menu");
    }

    private JButton menuButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(280, 46));
        button.setPreferredSize(new Dimension(280, 46));
        button.setFocusPainted(false);
        button.setBackground(new Color(41, 113, 120));
        button.setForeground(Color.WHITE);
        button.addActionListener(e -> action.run());
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        wrapper.setOpaque(false);
        wrapper.add(button);
        return button;
    }

    private void showNewGame() {
        JPanel panel = formPanel("Prepare for battle");
        JComboBox<ShipPreset> presetChoice = new JComboBox<>(presets.toArray(ShipPreset[]::new));
        presetChoice.setRenderer((list, value, index, selected, focus) ->
                new JLabel(value == null ? "" : value.name() + "  ·  " + value.id().substring(0, 8)));
        JSlider speed = slider(2, 10, 5);
        JSlider turnSpeed = slider(1, 18, 6);
        JSlider durability = slider(50, 300, 150);
        JSlider collisionReduction = slider(0, 90, 50);
        collisionReduction.setMajorTickSpacing(10);
        JSlider collisionDuration = slider(1, 10, 3);
        JSlider damage = slider(5, 40, 15);
        JSlider range = slider(200, 900, 480);
        addField(panel, "Player ship preset", presetChoice);
        addSlider(panel, "Ship speed", speed, "px / frame");
        addSlider(panel, "Turn speed", turnSpeed, "degrees / frame");
        addSlider(panel, "Ship durability", durability, "hull points");
        addSlider(panel, "Collision speed reduction", collisionReduction, "%");
        addSlider(panel, "Collision effect duration", collisionDuration, "seconds");
        addSlider(panel, "Cannon damage", damage, "damage / hit");
        addSlider(panel, "Cannon range", range, "pixels");
        JButton start = menuButton("Start voyage", () -> {
            ShipPreset chosen = (ShipPreset) presetChoice.getSelectedItem();
            if (chosen != null) startGame(chosen.id(), speed.getValue(), durability.getValue(),
                    turnSpeed.getValue(), collisionDuration.getValue(), collisionReduction.getValue(),
                    damage.getValue(), range.getValue(), null);
        });
        JPanel actions = new JPanel(new FlowLayout());
        actions.setOpaque(false);
        actions.add(start);
        actions.add(menuButton("Back", this::showMenu));
        panel.add(actions);
        showCard(panel, "new-game");
    }

    private static JSlider slider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setOpaque(false);
        slider.setMajorTickSpacing((max - min) / 4);
        slider.setPaintTicks(true);
        return slider;
    }

    private void addSlider(JPanel panel, String label, JSlider slider, String unit) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(Color.WHITE);
        JLabel value = new JLabel(slider.getValue() + " " + unit);
        value.setForeground(new Color(173, 211, 205));
        slider.addChangeListener(e -> value.setText(slider.getValue() + " " + unit));
        row.add(name, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        panel.add(row);
    }

    private void showLoadGames() {
        JPanel panel = formPanel("Saved voyages");
        List<GameStore.SavedGame> saves;
        try { saves = store.loadGames(); }
        catch (IOException | RuntimeException ex) { showError("Could not read saved games", ex); return; }
        if (saves.isEmpty()) {
            JLabel empty = new JLabel("No saved games yet. Start a voyage and save it from the pause menu.");
            empty.setForeground(Color.WHITE);
            panel.add(empty);
        } else {
            JList<GameStore.SavedGame> list = new JList<>(saves.toArray(GameStore.SavedGame[]::new));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(8);
            list.setCellRenderer((items, save, index, selected, focus) -> {
                JLabel label = new JLabel(save.name() + "   ·   Score " + save.score() + "   ·   ID " + save.id());
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(9, 10, 9, 10));
                label.setBackground(selected ? new Color(49, 119, 124) : PANEL);
                label.setForeground(Color.WHITE);
                return label;
            });
            panel.add(new JScrollPane(list));
            panel.add(menuButton("Load selected", () -> {
                GameStore.SavedGame selected = list.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(frame, "Choose a saved game first.");
                    return;
                }
                startGame(selected.playerPresetId(), selected.speed(), selected.playerMaxHealth(),
                        selected.turnSpeed(), selected.collisionDuration(), selected.collisionReduction(),
                        selected.damage(), selected.range(), selected);
            }));
        }
        panel.add(menuButton("Back", this::showMenu));
        showCard(panel, "load-games");
    }

    private void showEditor() {
        EditorPanel editor = new EditorPanel();
        showCard(editor, "editor");
    }

    private JPanel formPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(24, 32, 24, 32));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(18));
        return panel;
    }

    private void addField(JPanel panel, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(Color.WHITE);
        field.setPreferredSize(new Dimension(400, 32));
        row.add(name, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(10));
    }

    private void showCard(JPanel panel, String key) {
        root.removeAll();
        root.add(panel, key);
        cards.show(root, key);
        frame.revalidate();
    }

    private void startGame(String presetId, double speed, double hp, double turnSpeed,
                           double collisionDuration, double collisionReduction, double damage, double range,
                           GameStore.SavedGame saved) {
        ShipPreset playerPreset = findPreset(presetId);
        if (playerPreset == null) {
            JOptionPane.showMessageDialog(frame, "The saved player ship preset is missing. Choose another preset.",
                    "Missing preset", JOptionPane.ERROR_MESSAGE);
            if (saved != null) showLoadGames();
            return;
        }
        gamePanel = new GamePanel(playerPreset, speed, hp, turnSpeed, collisionDuration,
                collisionReduction, damage, range, saved);
        JPanel page = new JPanel(new CardLayout());
        page.add(gamePanel, "play");
        page.add(pauseMenu(page), "pause");
        gamePanel.setPauseAction(() -> ((CardLayout) page.getLayout()).show(page, "pause"));
        gamePanel.setResumeAction(() -> {
            ((CardLayout) page.getLayout()).show(page, "play");
            gamePanel.requestFocusInWindow();
        });
        gamePanel.setSaveAction(this::saveCurrentGame);
        gamePanel.setLoadAction(() -> {
            gamePanel.stop();
            showLoadGames();
        });
        gamePanel.setRestartAction(() -> {
            String id = gamePanel.player.preset.id();
            double gameSpeed = gamePanel.player.speed, gameHp = gamePanel.player.maxHealth;
            double gameTurnSpeed = gamePanel.player.turnSpeed;
            double gameCollisionDuration = gamePanel.collisionDuration;
            double gameCollisionReduction = gamePanel.collisionReduction;
            double gameDamage = gamePanel.player.damage, gameRange = gamePanel.player.range;
            gamePanel.stop();
            startGame(id, gameSpeed, gameHp, gameTurnSpeed, gameCollisionDuration,
                    gameCollisionReduction, gameDamage, gameRange, null);
        });
        gamePanel.setExitAction(() -> {
            gamePanel.stop();
            showMenu();
        });
        showCard(page, "game");
        SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
    }

    private JPanel pauseMenu(JPanel gamePage) {
        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setBackground(new Color(7, 25, 37));
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("PAUSED");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        title.setForeground(Color.WHITE);
        menu.add(title);
        menu.add(Box.createVerticalStrut(22));
        menu.add(menuButton("Resume", () -> gamePanel.resume()));
        menu.add(menuButton("Save current game", () -> gamePanel.save()));
        menu.add(menuButton("Restart current game", () -> gamePanel.restart()));
        menu.add(menuButton("Load a different game", () -> gamePanel.load()));
        menu.add(menuButton("Exit current game", () -> gamePanel.exit()));
        overlay.add(menu);
        return overlay;
    }

    private void saveCurrentGame() {
        if (gamePanel == null) return;
        String name = JOptionPane.showInputDialog(frame, "Name this saved game:",
                "Save voyage", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try {
            GameStore.SavedGame save = gamePanel.makeSave(name.trim());
            store.saveGame(save);
            JOptionPane.showMessageDialog(frame, "Saved with ID:\n" + save.id());
        } catch (IOException ex) { showError("Could not save the game", ex); }
    }

    private ShipPreset findPreset(String id) {
        return presets.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    }

    private void refreshPresets() throws IOException {
        store.savePresets(presets);
        presets = store.loadPresets();
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(frame, message + ":\n" + ex.getMessage(), "Ships",
                JOptionPane.ERROR_MESSAGE);
    }

    private final class EditorPanel extends JPanel {
        private final JComboBox<ShipPreset> presetsBox = new JComboBox<>();
        private final JTextField name = new JTextField(20);
        private final JSlider size = new JSlider(20, 90, 48);
        private final JComboBox<ShipPreset.State> state = new JComboBox<>(ShipPreset.State.values());
        private final JComboBox<ShipPreset.ShapeKind> shape = new JComboBox<>(ShipPreset.ShapeKind.values());
        private final JButton colorButton = new JButton("Choose color");
        private final Preview preview = new Preview();
        private int color = 0x35B8A6;
        private ShipPreset editing;

        EditorPanel() {
            setBackground(NAVY);
            setBorder(new EmptyBorder(24, 40, 24, 40));
            setLayout(new BorderLayout(25, 0));
            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Ship preset designer");
            title.setForeground(Color.WHITE);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            left.add(title);
            left.add(Box.createVerticalStrut(12));
            presetsBox.setRenderer((list, value, index, selected, focus) ->
                    new JLabel(value == null ? "" : value.name() + "  [" + value.id().substring(0, 8) + "]"));
            left.add(presetsBox);
            left.add(Box.createVerticalStrut(10));
            left.add(new JLabel("Preset name"));
            left.add(name);
            left.add(Box.createVerticalStrut(8));
            left.add(new JLabel("Ship size"));
            left.add(size);
            left.add(new JLabel("Durability appearance"));
            left.add(state);
            left.add(Box.createVerticalStrut(8));
            left.add(new JLabel("Body shape"));
            left.add(shape);
            left.add(Box.createVerticalStrut(8));
            left.add(colorButton);
            left.add(Box.createVerticalStrut(15));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
            actions.setOpaque(false);
            actions.add(menuButton("New preset", this::newPreset));
            actions.add(menuButton("Save preset", this::savePreset));
            actions.add(menuButton("Delete preset", this::deletePreset));
            left.add(actions);
            left.add(menuButton("Back to menu", ShipGame.this::showMenu));
            for (Component component : left.getComponents()) {
                if (component instanceof JLabel label) label.setForeground(new Color(213, 230, 225));
            }
            add(left, BorderLayout.CENTER);
            preview.setPreferredSize(new Dimension(300, 300));
            add(preview, BorderLayout.EAST);
            presetsBox.addActionListener(e -> loadSelected());
            state.addActionListener(e -> loadStateVisual());
            shape.addActionListener(e -> preview.repaint());
            size.addChangeListener(e -> preview.repaint());
            colorButton.addActionListener(e -> {
                Color selected = JColorChooser.showDialog(frame, "Choose ship color", new Color(color));
                if (selected != null) { color = selected.getRGB() & 0xFFFFFF; updateColorButton(); preview.repaint(); }
            });
            updateList();
        }

        private void updateList() {
            presetsBox.setModel(new DefaultComboBoxModel<>(presets.toArray(ShipPreset[]::new)));
            if (!presets.isEmpty()) presetsBox.setSelectedIndex(0);
        }

        private void loadSelected() {
            ShipPreset selected = (ShipPreset) presetsBox.getSelectedItem();
            if (selected == null) return;
            editing = selected;
            name.setText(selected.name());
            size.setValue((int) selected.size());
            loadStateVisual();
        }

        private void loadStateVisual() {
            if (editing == null) return;
            ShipPreset.Visual visual = editing.visuals().get(((ShipPreset.State) state.getSelectedItem()).name());
            if (visual == null) return;
            shape.setSelectedItem(visual.shape());
            color = visual.color();
            updateColorButton();
            preview.repaint();
        }

        private void updateColorButton() {
            colorButton.setBackground(new Color(color));
            colorButton.setForeground(new Color(color).getRed() * 0.299 + new Color(color).getGreen() * 0.587
                    + new Color(color).getBlue() * 0.114 > 145 ? Color.BLACK : Color.WHITE);
        }

        private void newPreset() {
            Map<String, ShipPreset.Visual> visuals = new LinkedHashMap<>();
            visuals.put("FULL", new ShipPreset.Visual(ShipPreset.ShapeKind.TRIANGLE, 0x35B8A6));
            visuals.put("HALF", new ShipPreset.Visual(ShipPreset.ShapeKind.TRIANGLE, 0xC18B42));
            visuals.put("SUNKEN", new ShipPreset.Visual(ShipPreset.ShapeKind.DIAMOND, 0x62666C));
            editing = new ShipPreset(UUID.randomUUID().toString(), "New Ship", 48, visuals);
            presets.add(editing);
            updateList();
            presetsBox.setSelectedItem(editing);
        }

        private void savePreset() {
            if (editing == null) return;
            if (name.getText().isBlank()) {
                JOptionPane.showMessageDialog(frame, "Enter a name for the preset.");
                return;
            }
            Map<String, ShipPreset.Visual> visuals = new LinkedHashMap<>(editing.visuals());
            visuals.put(((ShipPreset.State) state.getSelectedItem()).name(),
                    new ShipPreset.Visual((ShipPreset.ShapeKind) shape.getSelectedItem(), color));
            ShipPreset updated = new ShipPreset(editing.id(), name.getText().trim(), size.getValue(), visuals);
            int index = presets.indexOf(editing);
            if (index >= 0) presets.set(index, updated);
            editing = updated;
            try {
                refreshPresets();
                updateList();
                presetsBox.setSelectedItem(findPreset(updated.id()));
                JOptionPane.showMessageDialog(frame, "Preset saved. Unique ID:\n" + updated.id());
            } catch (IOException ex) { showError("Could not save presets", ex); }
        }

        private void deletePreset() {
            if (editing == null || presets.size() <= 1) {
                JOptionPane.showMessageDialog(frame, "Keep at least one preset.");
                return;
            }
            int result = JOptionPane.showConfirmDialog(frame, "Delete preset " + editing.name() + "?",
                    "Delete preset", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
            presets.remove(editing);
            try {
                refreshPresets();
                editing = null;
                updateList();
            } catch (IOException ex) { showError("Could not delete preset", ex); }
        }

        private final class Preview extends JPanel {
            Preview() { setOpaque(true); setBackground(SEA); }
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (editing == null) return;
                Graphics2D g = (Graphics2D) graphics.create();
                ShipPreset.Visual visual = new ShipPreset.Visual((ShipPreset.ShapeKind) shape.getSelectedItem(), color);
                Map<String, ShipPreset.Visual> visuals = new LinkedHashMap<>(editing.visuals());
                visuals.put(((ShipPreset.State) state.getSelectedItem()).name(), visual);
                ShipPreset previewPreset = new ShipPreset(editing.id(), name.getText().isBlank() ?
                        editing.name() : name.getText(), size.getValue(), visuals);
                double ratio = switch ((ShipPreset.State) state.getSelectedItem()) {
                    case FULL -> 1;
                    case HALF -> 0.5;
                    case SUNKEN -> 0;
                };
                Shape hull = previewPreset.shapeAt(getWidth() / 2.0, getHeight() / 2.0, -Math.PI / 2, ratio);
                g.setColor(new Color(color));
                g.fill(hull);
                g.setColor(new Color(230, 239, 228));
                g.setStroke(new BasicStroke(2));
                g.draw(hull);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                g.drawString(((ShipPreset.State) state.getSelectedItem()).name(), 15, 25);
                g.dispose();
            }
        }
    }

    private static class Player {
        ShipPreset preset;
        double x, y, angle, health, maxHealth, speed, turnSpeed, damage, range;
        double vx, vy;
        final CollisionSlowdown collisionSlowdown;
        Player(ShipPreset preset, double x, double y, double health, double speed, double turnSpeed,
               double damage, double range, double collisionDuration, double collisionReduction) {
            this.preset = preset;
            this.x = x;
            this.y = y;
            this.health = this.maxHealth = health;
            this.speed = speed;
            this.turnSpeed = turnSpeed;
            this.damage = damage;
            this.range = range;
            this.collisionSlowdown = new CollisionSlowdown(collisionDuration, collisionReduction);
        }
    }

    private static class Enemy {
        final ShipPreset preset;
        final CollisionSlowdown collisionSlowdown;
        double x, y, angle, vx, vy, health = 80, maxHealth = 80, fireWait, sinkingTime;
        EnemyBehavior behavior = new PursuitBehavior();
        Enemy(ShipPreset preset, double x, double y, double collisionDuration, double collisionReduction) {
            this.preset = preset;
            this.x = x;
            this.y = y;
            this.collisionSlowdown = new CollisionSlowdown(collisionDuration, collisionReduction);
        }
    }

    private static class Cannonball {
        double x, y, vx, vy, remaining;
        final boolean fromPlayer;
        Cannonball(double x, double y, double vx, double vy, double remaining, boolean fromPlayer) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.remaining = remaining; this.fromPlayer = fromPlayer;
        }
    }

    private static class TrailParticle {
        final boolean wave;
        double x, y, age, radius;
        final double lifetime;
        final Color color;

        TrailParticle(double x, double y, double radius, double lifetime, boolean wave, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.lifetime = lifetime;
            this.wave = wave;
            this.color = color;
        }
    }

    private static final class PursuitBehavior implements EnemyBehavior {
        public Point2D.Double direction(EnemyContext c) {
            return normalized(c.playerX() - c.x(), c.playerY() - c.y());
        }
    }
    private static final class FlankBehavior implements EnemyBehavior {
        public Point2D.Double direction(EnemyContext c) {
            double dx = c.playerX() - c.x(), dy = c.playerY() - c.y();
            return normalized(-dy + dx * 0.25, dx + dy * 0.25);
        }
    }
    private static final class RetreatBehavior implements EnemyBehavior {
        public Point2D.Double direction(EnemyContext c) {
            return normalized(c.x() - c.playerX(), c.y() - c.playerY());
        }
    }
    private static Point2D.Double normalized(double x, double y) {
        double length = Math.hypot(x, y);
        return length == 0 ? new Point2D.Double() : new Point2D.Double(x / length, y / length);
    }

    private final class GamePanel extends JPanel {
        private final javax.swing.Timer timer;
        private final Player player;
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Cannonball> cannonballs = new ArrayList<>();
        private final List<TrailParticle> trailParticles = new ArrayList<>();
        private final Map<Integer, Long> cooldowns = new HashMap<>();
        private final Map<ContactKey, Long> collisionCooldowns = new HashMap<>();
        private final Set<Integer> heldKeys = new HashSet<>();
        private final Random random = new Random();
        private int score;
        private double mouseX, mouseY;
        private double elapsed;
        private double wakeElapsed;
        private final double collisionDuration;
        private final double collisionReduction;
        private boolean paused;
        private boolean gameOver;
        private boolean steeringToMouse;
        private Runnable pauseAction, resumeAction, saveAction, loadAction, restartAction, exitAction;

        GamePanel(ShipPreset preset, double speed, double hp, double turnSpeed,
                  double collisionDuration, double collisionReduction, double damage, double range,
                  GameStore.SavedGame save) {
            setBackground(SEA);
            setFocusable(true);
            this.collisionDuration = collisionDuration;
            this.collisionReduction = collisionReduction;
            player = new Player(preset, 550, 380, hp, speed, turnSpeed, damage, range,
                    collisionDuration, collisionReduction);
            if (save == null) {
                mouseX = player.x; mouseY = player.y;
                for (int i = 0; i < 7; i++) spawnEnemy();
            } else {
                player.x = save.playerX(); player.y = save.playerY();
                player.health = save.playerHealth();
                score = save.score();
                mouseX = player.x; mouseY = player.y;
                for (GameStore.EnemySave enemy : save.enemies()) {
                    ShipPreset enemyPreset = findPreset(enemy.presetId());
                    if (enemyPreset != null) {
                        Enemy e = new Enemy(enemyPreset, enemy.x(), enemy.y(), collisionDuration, collisionReduction);
                        e.health = enemy.health();
                        e.maxHealth = enemy.maxHealth();
                        enemies.add(e);
                    }
                }
                while (enemies.size() < 3) spawnEnemy();
            }
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
                @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        steeringToMouse = true;
                        mouseX = e.getX();
                        mouseY = e.getY();
                    }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) steeringToMouse = false;
                }
                @Override public void mouseExited(MouseEvent e) {
                    if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) steeringToMouse = false;
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { steeringToMouse = false; }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { pause(); return; }
                    if (heldKeys.add(e.getKeyCode())) fire(e.getKeyCode());
                }
                @Override public void keyReleased(KeyEvent e) { heldKeys.remove(e.getKeyCode()); }
            });
            timer = new javax.swing.Timer(16, e -> tick());
            timer.start();
        }

        void setPauseAction(Runnable action) { pauseAction = action; }
        void setResumeAction(Runnable action) { resumeAction = action; }
        void setSaveAction(Runnable action) { saveAction = action; }
        void setLoadAction(Runnable action) { loadAction = action; }
        void setRestartAction(Runnable action) { restartAction = action; }
        void setExitAction(Runnable action) { exitAction = action; }
        void stop() { timer.stop(); }
        void resume() { paused = false; timer.start(); resumeAction.run(); }
        void pause() {
            if (paused) return;
            paused = true; timer.stop(); pauseAction.run();
        }
        void save() { saveAction.run(); }
        void load() { loadAction.run(); }
        void restart() { restartAction.run(); }
        void exit() { exitAction.run(); }

        private void fire(int key) {
            long now = System.currentTimeMillis();
            int cooldown = key == KeyEvent.VK_A || key == KeyEvent.VK_D ? 5000 : 10000;
            if (key != KeyEvent.VK_A && key != KeyEvent.VK_D && key != KeyEvent.VK_W && key != KeyEvent.VK_S) return;
            if (now < cooldowns.getOrDefault(key, 0L)) return;
            cooldowns.put(key, now + cooldown);
            double fx = Math.cos(player.angle), fy = Math.sin(player.angle);
            double leftX = Math.cos(player.angle - Math.PI / 2), leftY = Math.sin(player.angle - Math.PI / 2);
            double dirX, dirY;
            int count;
            if (key == KeyEvent.VK_W) { dirX = fx; dirY = fy; count = 2; }
            else if (key == KeyEvent.VK_S) { dirX = -fx; dirY = -fy; count = 3; }
            else if (key == KeyEvent.VK_A) { dirX = leftX; dirY = leftY; count = 5; }
            else { dirX = -leftX; dirY = -leftY; count = 5; }
            double sideX = -dirY, sideY = dirX;
            for (int i = 0; i < count; i++) {
                double spread = (i - (count - 1) / 2.0) * 9;
                double originX = player.x + sideX * spread + dirX * 22;
                double originY = player.y + sideY * spread + dirY * 22;
                cannonballs.add(new Cannonball(originX, originY, dirX * 5.5, dirY * 5.5, player.range, true));
            }
        }

        private void spawnEnemy() {
            if (presets.isEmpty()) return;
            double x = random.nextBoolean() ? random.nextInt(Math.max(1, getWidth())) : random.nextBoolean() ? 20 : getWidth() - 20;
            double y = random.nextBoolean() ? random.nextInt(Math.max(1, getHeight())) : random.nextBoolean() ? 60 : getHeight() - 30;
            if (getWidth() == 0 || getHeight() == 0) { x = 100 + random.nextInt(700); y = 100 + random.nextInt(400); }
            ShipPreset preset = presets.get(random.nextInt(presets.size()));
            enemies.add(new Enemy(preset, x, y, collisionDuration, collisionReduction));
        }

        private void tick() {
            if (paused) return;
            elapsed += 0.016;
            wakeElapsed += 0.016;
            double dx = mouseX - player.x, dy = mouseY - player.y;
            if (steeringToMouse && Math.hypot(dx, dy) > 3) {
                player.angle = TurnMath.rotateToward(player.angle, Math.atan2(dy, dx),
                        Math.toRadians(player.turnSpeed));
            } else if (Math.hypot(player.vx, player.vy) > 0.01) {
                player.angle = TurnMath.rotateToward(player.angle, Math.atan2(player.vy, player.vx),
                        Math.toRadians(player.turnSpeed));
            }
            Point2D.Double velocity = PlayerControls.velocityTowardMouse(
                    player.vx, player.vy, dx, dy, player.speed, steeringToMouse);
            player.vx = velocity.x;
            player.vy = velocity.y;
            player.x += player.vx * player.collisionSlowdown.speedMultiplier();
            player.y += player.vy * player.collisionSlowdown.speedMultiplier();
            player.collisionSlowdown.advance(0.016);
            ShipCollision.Body playerBody = body(player);
            ShipCollision.bounceAtBounds(playerBody, getWidth(), getHeight(), 40);
            applyBody(player, playerBody);

            for (Enemy enemy : enemies) {
                if (enemy.health <= 0) {
                    enemy.sinkingTime -= 0.016;
                    continue;
                }
                double distance = Point2D.distance(enemy.x, enemy.y, player.x, player.y);
                EnemyBehavior nextBehavior = enemy.health / enemy.maxHealth < 0.28
                        ? new RetreatBehavior() : distance < 210 ? new FlankBehavior() : new PursuitBehavior();
                if (!enemy.behavior.getClass().equals(nextBehavior.getClass())) enemy.behavior = nextBehavior;
                Point2D.Double direction = enemy.behavior.direction(new EnemyBehavior.EnemyContext(
                        enemy.x, enemy.y, player.x, player.y, enemy.health / enemy.maxHealth));
                direction = avoidShips(enemy, direction);
                if (Math.hypot(direction.x, direction.y) > 0) {
                    enemy.angle = TurnMath.rotateToward(enemy.angle, Math.atan2(direction.y, direction.x),
                            Math.toRadians(player.turnSpeed));
                    double movementSpeed = enemy.health / enemy.maxHealth < 0.28 ? 1.05 : 0.72;
                    enemy.vx = Math.cos(enemy.angle) * movementSpeed;
                    enemy.vy = Math.sin(enemy.angle) * movementSpeed;
                    enemy.x += enemy.vx * enemy.collisionSlowdown.speedMultiplier();
                    enemy.y += enemy.vy * enemy.collisionSlowdown.speedMultiplier();
                }
                enemy.collisionSlowdown.advance(0.016);
                if (wakeElapsed >= 0.09) addWake(enemy.x, enemy.y, enemy.angle, enemy.preset.size());
                ShipCollision.Body enemyBody = body(enemy);
                ShipCollision.bounceAtBounds(enemyBody, getWidth(), getHeight(), 40);
                applyBody(enemy, enemyBody);
                enemy.fireWait -= 0.016;
                if (enemy.fireWait <= 0 && distance < 520) {
                    Point2D.Double shot = normalized(player.x - enemy.x, player.y - enemy.y);
                    cannonballs.add(new Cannonball(enemy.x, enemy.y, shot.x * 2.7, shot.y * 2.7, 560, false));
                    enemy.fireWait = 2.3 + random.nextDouble();
                }
            }
            if (wakeElapsed >= 0.09) {
                addWake(player.x, player.y, player.angle, player.preset.size());
                wakeElapsed = 0;
            }
            resolveShipCollisions();
            List<Cannonball> remove = new ArrayList<>();
            for (Cannonball ball : cannonballs) {
                ball.x += ball.vx; ball.y += ball.vy; ball.remaining -= Math.hypot(ball.vx, ball.vy);
                trailParticles.add(new TrailParticle(ball.x - ball.vx * 0.8, ball.y - ball.vy * 0.8,
                        ball.fromPlayer ? 3 : 2.5, 0.48, false,
                        ball.fromPlayer ? new Color(173, 180, 174) : new Color(145, 156, 155)));
                Shape projectile = new Ellipse2D.Double(ball.x - 4, ball.y - 4, 8, 8);
                if (ball.fromPlayer) {
                    for (Enemy enemy : enemies) {
                        if (enemy.health > 0 && intersects(projectile, enemy.preset.shapeAt(enemy.x, enemy.y, enemy.angle,
                                enemy.health / enemy.maxHealth))) {
                            enemy.health -= player.damage;
                            remove.add(ball);
                            if (enemy.health <= 0) {
                                enemy.sinkingTime = 1.4;
                                score += 100;
                            }
                            break;
                        }
                    }
                } else if (intersects(projectile, player.preset.shapeAt(player.x, player.y, player.angle,
                        player.health / player.maxHealth))) {
                    player.health -= 8;
                    remove.add(ball);
                }
                if (ball.remaining <= 0 || ball.x < -15 || ball.y < -15 ||
                        ball.x > getWidth() + 15 || ball.y > getHeight() + 15) remove.add(ball);
            }
            cannonballs.removeAll(remove);
            updateTrailParticles();
            enemies.removeIf(enemy -> enemy.health <= 0 && enemy.sinkingTime <= 0);
            long now = System.currentTimeMillis();
            collisionCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
            while (enemies.size() < 7) spawnEnemy();
            if (player.health <= 0 && !gameOver) {
                player.health = 0;
                gameOver = true;
                timer.stop();
                repaint();
                javax.swing.Timer endDelay = new javax.swing.Timer(1200, e -> {
                    JOptionPane.showMessageDialog(frame, "Your ship has sunk. Final score: " + score,
                            "Voyage ended", JOptionPane.INFORMATION_MESSAGE);
                    exit();
                });
                endDelay.setRepeats(false);
                endDelay.start();
            }
            repaint();
        }

        private void addWake(double x, double y, double angle, double shipSize) {
            double rearX = x - Math.cos(angle) * shipSize * 0.42;
            double rearY = y - Math.sin(angle) * shipSize * 0.42;
            double sideX = Math.cos(angle + Math.PI / 2);
            double sideY = Math.sin(angle + Math.PI / 2);
            Color water = new Color(164, 225, 218);
            for (int side = -1; side <= 1; side++) {
                trailParticles.add(new TrailParticle(rearX + sideX * side * shipSize * 0.2,
                        rearY + sideY * side * shipSize * 0.2, 3 + Math.abs(side), 0.8, true, water));
            }
        }

        private void updateTrailParticles() {
            trailParticles.removeIf(particle -> {
                particle.age += 0.016;
                if (!particle.wave) {
                    particle.x += 0.04;
                    particle.y -= 0.08;
                }
                particle.radius += particle.wave ? 0.22 : 0.13;
                return particle.age >= particle.lifetime;
            });
        }

        private void paintTrailParticles(Graphics2D g) {
            for (TrailParticle particle : trailParticles) {
                float alpha = (float) Math.max(0, 1 - particle.age / particle.lifetime);
                g.setColor(new Color(particle.color.getRed(), particle.color.getGreen(),
                        particle.color.getBlue(), (int) (alpha * (particle.wave ? 115 : 145))));
                if (particle.wave) {
                    g.setStroke(new BasicStroke(1.4f));
                    int diameter = (int) Math.round(particle.radius * 2);
                    g.drawArc((int) Math.round(particle.x - particle.radius),
                            (int) Math.round(particle.y - particle.radius),
                            diameter, diameter, 15, 150);
                } else {
                    double diameter = particle.radius * 2;
                    g.fill(new Ellipse2D.Double(particle.x - particle.radius,
                            particle.y - particle.radius, diameter, diameter));
                }
            }
        }

        private Point2D.Double avoidShips(Enemy enemy, Point2D.Double goal) {
            double x = goal.x;
            double y = goal.y;
            double avoidDistance = enemy.preset.size() * 0.5 + player.preset.size() * 0.5 + 72;
            double awayX = enemy.x - player.x;
            double awayY = enemy.y - player.y;
            double distance = Math.hypot(awayX, awayY);
            if (distance < avoidDistance && distance > 0) {
                double urgency = (avoidDistance - distance) / avoidDistance;
                double force = 1.5 + urgency * 3.5;
                x += awayX / distance * force;
                y += awayY / distance * force;
            }
            for (Enemy other : enemies) {
                if (other == enemy || other.health <= 0) continue;
                awayX = enemy.x - other.x;
                awayY = enemy.y - other.y;
                distance = Math.hypot(awayX, awayY);
                avoidDistance = (enemy.preset.size() + other.preset.size()) * 0.5 + 56;
                if (distance < avoidDistance && distance > 0) {
                    double urgency = (avoidDistance - distance) / avoidDistance;
                    double force = 1.5 + urgency * 3.5;
                    x += awayX / distance * force;
                    y += awayY / distance * force;
                }
            }
            return normalized(x, y);
        }

        private void resolveShipCollisions() {
            long now = System.currentTimeMillis();
            for (int i = 0; i < enemies.size(); i++) {
                Enemy first = enemies.get(i);
                if (first.health <= 0) continue;
                collide(player, first, now);
                for (int j = i + 1; j < enemies.size(); j++) {
                    if (first.health <= 0) break;
                    Enemy second = enemies.get(j);
                    if (second.health > 0) collide(first, second, now);
                }
            }
        }

        private void collide(Player first, Enemy second, long now) {
            if (!intersects(shipShape(first), shipShape(second))) return;
            ShipCollision.Body a = body(first), b = body(second);
            if (!ShipCollision.bounce(a, b)) return;
            applyBody(first, a);
            applyBody(second, b);
            damageOnCollision(first, second, now);
        }

        private void collide(Enemy first, Enemy second, long now) {
            if (!intersects(shipShape(first), shipShape(second))) return;
            ShipCollision.Body a = body(first), b = body(second);
            if (!ShipCollision.bounce(a, b)) return;
            applyBody(first, a);
            applyBody(second, b);
            damageOnCollision(first, second, now);
        }

        private void damageOnCollision(Object first, Object second, long now) {
            ContactKey key = new ContactKey(first, second);
            if (now < collisionCooldowns.getOrDefault(key, 0L)) return;
            collisionCooldowns.put(key, now + 900);
            if (first instanceof Player ship) {
                ship.health -= 3;
                ship.collisionSlowdown.trigger();
            }
            else if (first instanceof Enemy ship) {
                ship.health -= 3;
                ship.collisionSlowdown.trigger();
                markSunk(ship);
            }
            if (second instanceof Player ship) {
                ship.health -= 3;
                ship.collisionSlowdown.trigger();
            }
            else if (second instanceof Enemy ship) {
                ship.health -= 3;
                ship.collisionSlowdown.trigger();
                markSunk(ship);
            }
        }

        private void markSunk(Enemy enemy) {
            if (enemy.health <= 0 && enemy.sinkingTime <= 0) {
                enemy.sinkingTime = 1.4;
                score += 100;
            }
        }

        private Shape shipShape(Player ship) {
            return ship.preset.shapeAt(ship.x, ship.y, ship.angle, ship.health / ship.maxHealth);
        }

        private Shape shipShape(Enemy ship) {
            return ship.preset.shapeAt(ship.x, ship.y, ship.angle, ship.health / ship.maxHealth);
        }

        private ShipCollision.Body body(Player ship) {
            return new ShipCollision.Body(ship.x, ship.y, ship.vx, ship.vy, ship.preset.size() / 2);
        }

        private ShipCollision.Body body(Enemy ship) {
            return new ShipCollision.Body(ship.x, ship.y, ship.vx, ship.vy, ship.preset.size() / 2);
        }

        private void applyBody(Player ship, ShipCollision.Body body) {
            ship.x = body.x;
            ship.y = body.y;
            ship.vx = body.vx;
            ship.vy = body.vy;
        }

        private void applyBody(Enemy ship, ShipCollision.Body body) {
            ship.x = body.x;
            ship.y = body.y;
            ship.vx = body.vx;
            ship.vy = body.vy;
            if (Math.hypot(body.vx, body.vy) > 0) {
                ship.angle = Math.atan2(body.vy, body.vx);
            }
        }

        private boolean intersects(Shape a, Shape b) {
            Area overlap = new Area(a);
            overlap.intersect(new Area(b));
            return !overlap.isEmpty();
        }

        private final class ContactKey {
            private final Object first;
            private final Object second;

            ContactKey(Object first, Object second) {
                this.first = first;
                this.second = second;
            }

            @Override public boolean equals(Object value) {
                if (!(value instanceof ShipGame.GamePanel.ContactKey other)) return false;
                return first == other.first && second == other.second
                        || first == other.second && second == other.first;
            }

            @Override public int hashCode() {
                return System.identityHashCode(first) ^ System.identityHashCode(second);
            }
        }

        private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

        GameStore.SavedGame makeSave(String name) {
            List<GameStore.EnemySave> state = enemies.stream().map(e ->
                    new GameStore.EnemySave(e.preset.id(), e.x, e.y, e.health, e.maxHealth)).toList();
            return new GameStore.SavedGame(UUID.randomUUID().toString(), name, player.preset.id(),
                    player.x, player.y, player.health, player.maxHealth, player.speed, player.turnSpeed,
                    collisionDuration, collisionReduction, player.damage, player.range, score, state);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintOceanWaves(g);
            paintTrailParticles(g);
            for (Enemy enemy : enemies) {
                Shape body = enemy.preset.shapeAt(enemy.x, enemy.y, enemy.angle, enemy.health / enemy.maxHealth);
                g.setColor(new Color(enemy.preset.visualFor(enemy.health / enemy.maxHealth).color()));
                g.fill(body);
                g.setColor(new Color(15, 35, 43));
                g.setStroke(new BasicStroke(2));
                g.draw(body);
                drawHealth(g, enemy.x - 22, enemy.y - enemy.preset.size() / 2 - 10, 44,
                        enemy.health / enemy.maxHealth);
            }
            Shape playerBody = player.preset.shapeAt(player.x, player.y, player.angle,
                    player.health / player.maxHealth);
            g.setColor(new Color(player.preset.visualFor(player.health / player.maxHealth).color()));
            g.fill(playerBody);
            g.setColor(new Color(237, 244, 223));
            g.setStroke(new BasicStroke(2));
            g.draw(playerBody);
            for (Cannonball ball : cannonballs) {
                g.setColor(ball.fromPlayer ? new Color(255, 224, 143) : new Color(255, 122, 102));
                g.fill(new Ellipse2D.Double(ball.x - 4, ball.y - 4, 8, 8));
            }
            paintSkyTint(g);
            paintHud(g);
            g.dispose();
        }

        private void paintSkyTint(Graphics2D g) {
            if (getHeight() <= 0) return;
            Graphics2D tint = (Graphics2D) g.create();
            tint.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(),
                    new float[]{0, 1},
                    new Color[]{SkyCycle.topTint(elapsed), SkyCycle.horizonTint(elapsed)}));
            tint.fillRect(0, 0, getWidth(), getHeight());
            tint.dispose();
        }

        private void paintOceanWaves(Graphics2D g) {
            g.setColor(new Color(157, 218, 215, 36));
            g.setStroke(new BasicStroke(1.2f));
            double phase = elapsed * 0.8;
            for (int row = -1; row < getHeight() / 46 + 2; row++) {
                double baseY = row * 46 + 18;
                Path2D wave = new Path2D.Double();
                for (int x = -12; x <= getWidth() + 12; x += 12) {
                    double along = x * 0.014 + phase + row * 0.63;
                    double y = baseY + Math.sin(along) * 5 + Math.sin(along * 0.43 + phase * 0.7) * 2.2;
                    if (x == -12) wave.moveTo(x, y);
                    else wave.lineTo(x, y);
                }
                g.draw(wave);
            }
        }

        private void drawHealth(Graphics2D g, double x, double y, double width, double ratio) {
            g.setColor(new Color(10, 23, 29, 190));
            g.fillRoundRect((int) x, (int) y, (int) width, 5, 5, 5);
            g.setColor(ratio > 0.5 ? new Color(113, 211, 155) : new Color(231, 149, 91));
            g.fillRoundRect((int) x, (int) y, (int) Math.max(0, width * ratio), 5, 5, 5);
        }

        private void paintHud(Graphics2D g) {
            g.setColor(new Color(7, 25, 37, 220));
            g.fillRoundRect(16, 14, 285, 87, 12, 12);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString("HULL", 29, 38);
            drawHealth(g, 78, 28, 200, player.health / player.maxHealth);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            g.drawString("Score: " + score + "     Enemies: " + enemies.size(), 29, 62);
            g.drawString("W  " + cooldownText(KeyEvent.VK_W) + "   S  " + cooldownText(KeyEvent.VK_S)
                    + "   A  " + cooldownText(KeyEvent.VK_A) + "   D  " + cooldownText(KeyEvent.VK_D), 29, 84);
            g.setColor(new Color(255, 255, 255, 190));
            g.drawString("W front · S rear · A port · D starboard · Esc pause", 18, getHeight() - 16);
        }

        private String cooldownText(int key) {
            long remaining = cooldowns.getOrDefault(key, 0L) - System.currentTimeMillis();
            return remaining <= 0 ? "Ready" : String.format(Locale.ROOT, "%.1fs", remaining / 1000.0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new ShipGame(); }
            catch (IOException | RuntimeException ex) {
                JOptionPane.showMessageDialog(null, "Could not start Ships:\n" + ex.getMessage(),
                        "Startup error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
    }
}
