package com.ships;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

final class PirateShipRenderer {
    static final int CANNONS_PER_SIDE = 5;
    static final int FRONT_CANNONS = 2;
    static final int BACK_CANNONS = 3;

    private static final Color OUTLINE = new Color(39, 30, 24);
    private static final Color DECK = new Color(101, 66, 42);
    private static final Color DECK_EDGE = new Color(191, 145, 86);
    private static final Color CANVAS = new Color(235, 224, 188);
    private static final Color CANNON = new Color(42, 43, 40);
    private static final Color BRASS = new Color(200, 158, 73);

    private PirateShipRenderer() {}

    static void draw(Graphics2D graphics, ShipPreset preset, double x, double y,
                     double angle, double durabilityRatio) {
        Graphics2D ship = (Graphics2D) graphics.create();
        ship.translate(x, y);
        ship.rotate(angle);
        ship.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape hull = preset.shapeAt(0, 0, 0, durabilityRatio);
        ship.setColor(new Color(preset.visualFor(durabilityRatio).color()));
        ship.fill(hull);
        ship.setColor(OUTLINE);
        ship.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        ship.draw(hull);

        Graphics2D details = (Graphics2D) ship.create();
        details.scale(preset.length() / 48.0, preset.width() / 28.0);
        drawDeck(details);
        drawSideCannons(details);
        drawEndCannons(details, FRONT_CANNONS, 1);
        drawEndCannons(details, BACK_CANNONS, -1);
        drawMastAndSail(details);
        drawRudder(details);
        details.dispose();
        ship.dispose();
    }

    private static void drawDeck(Graphics2D g) {
        Path2D deck = new Path2D.Double();
        deck.moveTo(13, 0);
        deck.lineTo(8, -5.2);
        deck.lineTo(-9, -5.2);
        deck.lineTo(-15, 0);
        deck.lineTo(-9, 5.2);
        deck.lineTo(8, 5.2);
        deck.closePath();
        g.setColor(DECK);
        g.fill(deck);
        g.setColor(DECK_EDGE);
        g.setStroke(new BasicStroke(1.1f));
        g.draw(deck);
        g.setColor(new Color(58, 39, 29, 180));
        g.setStroke(new BasicStroke(0.65f));
        for (int plank = -3; plank <= 3; plank += 2) {
            g.drawLine(-9, plank, 9, plank);
        }
    }

    private static void drawSideCannons(Graphics2D g) {
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int cannon = 0; cannon < CANNONS_PER_SIDE; cannon++) {
            double x = -11 + cannon * (22.0 / (CANNONS_PER_SIDE - 1));
            drawCannon(g, x, -9.2, x, -15, 1);
            drawCannon(g, x, 9.2, x, 15, 1);
        }
    }

    private static void drawEndCannons(Graphics2D g, int count, double direction) {
        g.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int cannon = 0; cannon < count; cannon++) {
            double y = (cannon - (count - 1) / 2.0) * 5.2;
            double x = direction * 20;
            drawCannon(g, x, y, x + direction * 5.5, y, 0.68);
        }
    }

    private static void drawCannon(Graphics2D g, double x1, double y1, double x2, double y2, double scale) {
        Stroke previous = g.getStroke();
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke((float) (3.4 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
        g.setColor(CANNON);
        g.setStroke(new BasicStroke((float) (2.2 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
        g.setColor(BRASS);
        g.fillOval((int) Math.round(x1 - 1.6 * scale), (int) Math.round(y1 - 1.6 * scale),
                (int) Math.ceil(3.2 * scale), (int) Math.ceil(3.2 * scale));
        g.setStroke(previous);
    }

    private static void drawMastAndSail(Graphics2D g) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(0, -10, 0, 10);
        Path2D sail = new Path2D.Double();
        sail.moveTo(-1.4, -8.4);
        sail.lineTo(-1.4, 7.1);
        sail.lineTo(-9.2, 3.4);
        sail.closePath();
        g.setColor(CANVAS);
        g.fill(sail);
        g.setColor(new Color(105, 55, 43));
        g.setStroke(new BasicStroke(0.85f));
        g.draw(sail);
        g.draw(new Line2D.Double(-2.4, -7.3, -7.2, 2.9));
        Path2D pirateFlag = new Path2D.Double();
        pirateFlag.moveTo(0, -10);
        pirateFlag.lineTo(6, -10);
        pirateFlag.lineTo(0, -7.1);
        pirateFlag.closePath();
        g.setColor(new Color(35, 35, 33));
        g.fill(pirateFlag);
        g.setColor(CANVAS);
        g.fill(new Ellipse2D.Double(0, -9.8, 1.8, 1.8));
        g.setStroke(new BasicStroke(0.45f));
        g.draw(new Line2D.Double(0.3, -7.8, 1.7, -6.7));
        g.draw(new Line2D.Double(1.7, -7.8, 0.3, -6.7));
    }

    private static void drawRudder(Graphics2D g) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(-18, 0, -21.5, 0));
        Path2D rudder = new Path2D.Double();
        rudder.moveTo(-21, -1.1);
        rudder.lineTo(-25, -2.6);
        rudder.lineTo(-24, 0);
        rudder.lineTo(-25, 2.6);
        rudder.lineTo(-21, 1.1);
        rudder.closePath();
        g.setColor(DECK);
        g.fill(rudder);
        g.setColor(OUTLINE);
        g.draw(rudder);
    }
}
