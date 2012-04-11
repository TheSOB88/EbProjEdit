package ebhack;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import ebhack.MapEditor.MapData.Sector;

public class MapEditor extends ToolModule implements ActionListener, DocumentListener, AdjustmentListener, MouseWheelListener {
	
	private JTextField xField, yField;
	private JComboBox tilesetChooser, palChooser, musicChooser;
	private JScrollBar xScroll, yScroll;
	private JMenu modeMenu;
	private JMenuItem sectorProps, findSprite, copySector, pasteSector, undo;
	
	public static MapData map;
	private MapDisplay mapDisplay;
	private TileSelector tileSelector;
	
	private MapData.Sector copiedSector;
	private int[][] copiedSectorTiles = new int[4][8];
	
	public MapEditor(YMLPreferences prefs) {
		super(prefs);
		
		map = new MapData();
	}

	public String getDescription() {
		return "Map Editor";
	}

	public String getVersion() {
		return "0.1";
	}

	public String getCredits() {
		return "Written by MrTenda";
	}

	public void init() {
        mainWindow = createBaseWindow(this);
        mainWindow.setTitle(this.getDescription());
        
        JMenuBar menuBar = new JMenuBar();
		ButtonGroup group = new ButtonGroup();
		JCheckBoxMenuItem checkBox;
		JRadioButtonMenuItem radioButton;
		JMenu menu;

		menu = new JMenu("File");
		menu.add(ToolModule.createJMenuItem("Apply Changes", 's', null,
				"apply", this));
		menu.add(ToolModule.createJMenuItem("Exit", 'x', null,
				"close", this));
		menuBar.add(menu);

		menu = new JMenu("Edit");
		undo = ToolModule.createJMenuItem("Undo Tile Change", 'u', null,
				"undoMap", this);
		undo.setEnabled(false);
		//menu.add(undo);
		copySector = ToolModule.createJMenuItem("Copy Sector", 'c', null,
				"copySector", this);
		menu.add(copySector);
		pasteSector = ToolModule.createJMenuItem("Paste Sector", 'p', null,
				"pasteSector", this);
		menu.add(pasteSector);
		sectorProps = ToolModule.createJMenuItem("Edit Sector's Properties",
				'r', null, "sectorEdit", this);
		//menu.add(sectorProps);
		menuBar.add(menu);

		modeMenu = new JMenu("Mode");
		group = new ButtonGroup();
		radioButton = new JRadioButtonMenuItem("Map Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode0");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Sprite Edit");
		radioButton.setSelected(false);
		radioButton.setActionCommand("mode1");
		radioButton.addActionListener(this);
		//group.add(radioButton);
		//modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Door Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode2");
		radioButton.addActionListener(this);
		//group.add(radioButton);
		//modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Hotspot Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode6");
		radioButton.addActionListener(this);
		//group.add(radioButton);
		//modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Enemy Edit");
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode7");
		radioButton.addActionListener(this);
		//group.add(radioButton);
		//modeMenu.add(radioButton);
		menuBar.add(modeMenu);

		menu = new JMenu("Tools");
		findSprite = ToolModule.createJMenuItem("Find Sprite Entry", 'f',
				null, "findSprite", this);
		menu.add(findSprite);
		menu.add(new JSeparator());
		menu.add(ToolModule.createJMenuItem("Clear Map", 'm', null,
				"delAllMap", this));
		//menu.add(ToolModule.createJMenuItem("Delete All Sprites", 's', null,
		//		"delAllSprites", this));
		//menu.add(ToolModule.createJMenuItem("Delete All Doors", 'o', null,
		//		"delAllDoors", this));
		//menu.add(ToolModule.createJMenuItem("Clear Enemy Placements", 'e', null,
		//		"delAllEnemies", this));
		menu.add(new JSeparator());
		menu.add(ToolModule.createJMenuItem("Clear Tile Image Cache", 't',
				null, "resetTileImages", this));
		menuBar.add(menu);

		menu = new JMenu("Options");
		checkBox = new JCheckBoxMenuItem("Show Grid");
		checkBox.setMnemonic('g');
		checkBox.setSelected(true);
		checkBox.setActionCommand("grid");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Sprite Boxes");
		checkBox.setMnemonic('b');
		checkBox.setSelected(true);
		checkBox.setActionCommand("spriteboxes");
		checkBox.addActionListener(this);
		//menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Enemy Sprites");
		checkBox.setMnemonic('e');
		checkBox.setSelected(true);
		checkBox.setActionCommand("enemySprites");
		checkBox.addActionListener(this);
		//menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Enemy Colors");
		checkBox.setMnemonic('l');
		checkBox.setSelected(true);
		checkBox.setActionCommand("enemycolors");
		checkBox.addActionListener(this);
		//menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Map Changes");
		checkBox.setMnemonic('c');
		checkBox.setSelected(false);
		checkBox.setActionCommand("mapchanges");
		checkBox.addActionListener(this);
		//menu.add(checkBox);
		menuBar.add(menu);
		
		mainWindow.setJMenuBar(menuBar);

		JPanel contentPanel = new JPanel(new BorderLayout());
        
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel("X: "));
		xField = ToolModule.createSizedJTextField(Integer.toString(MapData.WIDTH_IN_TILES).length(), true);
		xField.getDocument().addDocumentListener(this);
		panel.add(xField);
		panel.add(new JLabel("Y: "));
		yField = ToolModule.createSizedJTextField(Integer.toString(MapData.HEIGHT_IN_TILES).length(), true);
		panel.add(yField);
		panel.add(new JLabel("Tileset: "));
		tilesetChooser = new JComboBox();
		tilesetChooser.addActionListener(this);
		panel.add(tilesetChooser);
		loadTilesetNames();
		panel.add(new JLabel("Palette: "));
		palChooser = new JComboBox();
		palChooser.addActionListener(this);
		panel.add(palChooser);
		panel.add(new JLabel("Music: "));
		// TODO use ToolModule.creatJComboBoxFromArray() here
		musicChooser = new JComboBox();
		musicChooser.addActionListener(this);
		panel.add(musicChooser);
		loadMusicNames();
		contentPanel.add(panel, BorderLayout.NORTH);
		
		tilesetChooser.setEnabled(false);
		musicChooser.setEnabled(false);
		
		mapDisplay = new MapDisplay(map, copySector, pasteSector);
		mapDisplay.addMouseWheelListener(this);
		mapDisplay.addActionListener(this);
		mapDisplay.init();
		contentPanel.add(mapDisplay, BorderLayout.CENTER);
		
		xScroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, mapDisplay.getScreenWidth(), 0, MapData.WIDTH_IN_TILES);
		xScroll.addAdjustmentListener(this);
		contentPanel.add(xScroll, BorderLayout.SOUTH);
		yScroll = new JScrollBar(JScrollBar.VERTICAL, 0, mapDisplay.getScreenHeight(), 0, MapData.HEIGHT_IN_TILES);
		yScroll.addAdjustmentListener(this);
		contentPanel.add(yScroll, BorderLayout.EAST);
		
		mainWindow.getContentPane().add(contentPanel, BorderLayout.NORTH);
		
		tileSelector = new TileSelector(24, 3);
		mapDisplay.setTileSelector(tileSelector);
		mainWindow.getContentPane().add(
				ToolModule.pairComponents(tileSelector, tileSelector.getScrollBar(), false),
				BorderLayout.CENTER);
        
        mainWindow.invalidate();
        mainWindow.pack();
        //mainWindow.setSize(300, 400);
        mainWindow.setLocationByPlatform(true);
        mainWindow.validate();
        mainWindow.setResizable(false);
	}
	
	private void loadTilesetNames() {
		tilesetChooser.removeActionListener(this);
		tilesetChooser.removeAllItems();
		for (int i = 0; i < MapData.NUM_MAP_TSETS; i++)
			tilesetChooser.addItem(getNumberedString(
					TileEditor.TILESET_NAMES[TileEditor.getDrawTilesetNumber(i)], i,
					false));
		tilesetChooser.addActionListener(this);
	}
	
	// TODO make it actually load names from musiclisting.txt
	public void loadMusicNames() {
		musicChooser.removeActionListener(this);
		musicChooser.removeAllItems();
		for (int i = 0; i < 164; i++)
			musicChooser.addItem(getNumberedString("???", i, false));
		musicChooser.addActionListener(this);
	}
	
	public void show() {
		super.show();
		
		mainWindow.setVisible(true);
	}
	
	public void hide() {
		if (isInited)
			mainWindow.setVisible(false);
	}
	
	public static class MapDisplay extends AbstractButton implements MouseListener {
		private MapData map;
		private JMenuItem copySector, pasteSector;
		
		private final ActionEvent sectorEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "sectorChanged");
		
		private static Image[][][] tileImageCache;
		
		private int screenWidth = 24;
		private int screenHeight = 12;
		
		// Map X and Y coordinates of the tile displayed in the top left corner
		private int x = 0, y = 0;
		// Data for the selected sector
		private MapData.Sector selectedSector = null;
		private int sectorX, sectorY;
		private int sectorPal;
		private boolean grid = true;
		private boolean drawSprites = true;
		
		private TileSelector tileSelector;
		
		public MapDisplay(MapData map, JMenuItem copySector, JMenuItem pasteSector) {
			super();
			this.map = map;
			this.copySector = copySector;
			this.pasteSector = pasteSector;
			
			if (tileImageCache == null)
				resetTileImageCache();
			
			addMouseListener(this);
			
			setPreferredSize(new Dimension(
					screenWidth * MapData.TILE_WIDTH + 2,
					screenHeight * MapData.TILE_HEIGHT + 2));
		}
		
		public void init() {
			selectSector(0,0);
		}
		
		public void setTileSelector(TileSelector tileSelector) {
			this.tileSelector = tileSelector;
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			if (isEnabled())
				drawMap(g2d);

			// Draw border
			g2d.setColor(Color.black);
			g2d.draw(new Rectangle2D.Double(0, 0,
					screenWidth * MapData.TILE_WIDTH + 1,
					screenHeight * MapData.TILE_HEIGHT + 1));
		}
		
		private void drawMap(Graphics2D g) {
			g.setPaint(Color.white);
			g.setFont(new Font("Arial", Font.PLAIN, 12));
			
			MapData.Sector sector;
			int pal;
			for (int i = 0; i < screenHeight; i++) {
				for (int j = 0; j < screenWidth; j++) {

					sector = map.getSector(
							(j + x)/MapData.SECTOR_WIDTH,
							(i + y)/MapData.SECTOR_HEIGHT);
					pal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(sector.tileset)].getPaletteNum(
							sector.tileset, sector.palette);
					g.drawImage(
							getTileImage(TileEditor.getDrawTilesetNumber(sector.tileset),
									map.getMapTile(x+j, y+i), pal),
							j*MapData.TILE_WIDTH + 1, i*MapData.TILE_HEIGHT + 1,
							MapData.TILE_WIDTH, MapData.TILE_HEIGHT,
							this);
				}
			}
			
			if (grid)
				drawGrid(g);
			
			if (selectedSector != null) {
				int sXt, sYt;
				if (((sXt = sectorX * MapData.SECTOR_WIDTH) + MapData.SECTOR_WIDTH >= x)
						&& (sXt < x+screenWidth)
						&& ((sYt = sectorY * MapData.SECTOR_HEIGHT) + MapData.SECTOR_HEIGHT >= y)
						&& (sYt < y+screenHeight)) {
					g.setPaint(Color.yellow);
					g.draw(new Rectangle2D.Double(
							(sXt - x) * MapData.TILE_WIDTH + 1,
							(sYt - y) * MapData.TILE_HEIGHT + 1,
							MapData.SECTOR_WIDTH * MapData.TILE_WIDTH,
							MapData.SECTOR_HEIGHT * MapData.TILE_HEIGHT));
				}
			}
			
			MapData.NPC npc;
			int w, h;
			if (drawSprites) {
				g.setPaint(Color.RED);
				List<MapData.SpriteEntry> area;
				for (int i = y&(~7); i < (y&(~7)) + screenHeight + 8; i += 8) {
					for (int j = x&(~7); j < (x&(~7)) + screenWidth + 8; j += 8) {
						try {
							area = map.getSpriteArea(j>>3, i>>3);
							for (MapData.SpriteEntry e : area) {
								npc = map.getNPC(e.npcID);
								w = SpriteLoader.getSpriteW(npc.sprite);
								h = SpriteLoader.getSpriteH(npc.sprite);
								g.draw(new Rectangle2D.Double(
										e.x + (j-x)*MapData.TILE_WIDTH - w/2,
										e.y + (i-y)*MapData.TILE_HEIGHT - h/2,
										w, h));
								g.drawImage(SpriteLoader.getSprite(npc.sprite, npc.direction),
										e.x + (j-x)*MapData.TILE_WIDTH - w/2,
										e.y + (i-y)*MapData.TILE_HEIGHT - h/2,
										this);
							}
						} catch (Exception e) {
							
						}
					}
				}
			}
		}
		
		private void drawSprite(Graphics2D g, int n, int dir, int x, int y) {
			g.setColor(Color.red);
			int w = SpriteLoader.getSpriteW(n), h = SpriteLoader.getSpriteH(n);
			g.draw(new Rectangle2D.Double(
					x - w/2, y - h/2, w, h));
			
			g.drawImage(SpriteLoader.getSprite(n, dir),
					x - SpriteLoader.getSpriteW(n)/2,
					y - SpriteLoader.getSpriteH(n)/2,
					this);
		}
		
		private void drawGrid(Graphics2D g) {
			g.setPaint(Color.black);
			// Draw vertical lines
			for (int i = 0; i < screenWidth+1; i++)
				g.drawLine(1 + i * MapData.TILE_WIDTH, 1, 1 + i * MapData.TILE_WIDTH, screenHeight * MapData.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < screenHeight+1; i++)
				g.drawLine(1, 1 + i * MapData.TILE_HEIGHT, screenWidth * MapData.TILE_WIDTH, 1 + i * MapData.TILE_HEIGHT);
			
			// Blank pixel in the bottom right corner
			g.drawLine(screenWidth * MapData.TILE_WIDTH + 1,
					screenHeight * MapData.TILE_HEIGHT + 1,
					screenWidth * MapData.TILE_WIDTH + 1,
					screenHeight * MapData.TILE_HEIGHT + 1);
		}
		
		public static Image getTileImage(int loadtset, int loadtile, int loadpalette) {
			if (tileImageCache[loadtset][loadtile][loadpalette] == null) {
				tileImageCache[loadtset][loadtile][loadpalette] =
					TileEditor.tilesets[loadtset].getArrangementImage(loadtile, loadpalette);
			}
			return tileImageCache[loadtset][loadtile][loadpalette];
		}

		public static void resetTileImageCache() {
			tileImageCache = new Image[TileEditor.NUM_TILESETS][1024][59];
		}
		
		public int getScreenWidth() {
			return screenWidth;
		}
		
		public int getScreenHeight() {
			return screenHeight;
		}
		
		public MapData.Sector getSelectedSector() {
			return selectedSector;
		}
		
		public void setSelectedSectorTileset(int tset) {
			selectedSector.tileset = tset;
			sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)].getPaletteNum(
					selectedSector.tileset, selectedSector.palette);
		}
		
		public void setSelectedSectorPalette(int pal) {
			selectedSector.palette = pal;
			sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)].getPaletteNum(
					selectedSector.tileset, selectedSector.palette);
		}
		
		public int getSelectedSectorPalNumber() {
			return sectorPal;
		}
		
		public void setMapXY(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public void setMapY(int y) {
			this.y = y;
		}
		
		public int getMapX() {
			return x;
		}
		
		public int getMapY() {
			return y;
		}
		
		public int getSectorX() {
			return sectorX;
		}
		
		public int getSectorY() {
			return sectorY;
		}
		
		private void selectSector(int sX, int sY) {
			sectorX = sX;
			sectorY = sY;
			MapData.Sector newS = map.getSector(sectorX, sectorY);
			if (selectedSector != newS) {
				selectedSector = newS;
				sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)].getPaletteNum(
						selectedSector.tileset, selectedSector.palette);
				copySector.setEnabled(true);
				pasteSector.setEnabled(true);
			} else {
				// Un-select sector
				selectedSector = null;
				copySector.setEnabled(false);
				pasteSector.setEnabled(false);
			}
			repaint();
			this.fireActionPerformed(sectorEvent);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// Make sure they didn't click on the border
			if ((e.getX() >= 1) && (e.getX() <= screenWidth * MapData.TILE_WIDTH + 2)
					&& (e.getY() >= 1) && (e.getY() <= screenHeight * MapData.TILE_HEIGHT + 2)) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					int mX = (e.getX() - 1) / MapData.TILE_WIDTH + x;
					int mY = (e.getY() - 1) / MapData.TILE_HEIGHT + y;
					if (e.isShiftDown()) {
						tileSelector.selectTile(map.getMapTile(mX, mY));
					} else {
						map.setMapTile(mX, mY, tileSelector.getSelectedTile());
						repaint();
					}
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					// Make sure they didn't click on the border
					int sX = (x + ((e.getX() - 1) / MapData.TILE_WIDTH)) / MapData.SECTOR_WIDTH;
					int sY = (y + ((e.getY() - 1) / MapData.TILE_HEIGHT)) / MapData.SECTOR_HEIGHT;
					selectSector(sX, sY);
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void changeMode(int i) {
			// TODO Auto-generated method stub
			
		}

		public void toggleGrid() {
			grid = !grid;
		}

		public void toggleSpriteBoxes() {
			// TODO Auto-generated method stub
			
		}

		public void toggleMapChanges() {
			// TODO Auto-generated method stub
			
		}

		public void undoMapAction() {
			// TODO Auto-generated method stub
			
		}
	}
	
    private class TileSelector extends AbstractButton implements MouseListener, AdjustmentListener {
    	private int width, height;
    	private int tile = 0;
    	private JScrollBar scroll;
    	
    	public TileSelector(int width, int height) {
    		super();
    		
    		this.width = width;
    		this.height = height;
    		
			scroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, width, 0, (1024/height) + (1024%height > 0 ? 1 : 0));
			scroll.addAdjustmentListener(this);
			
			setPreferredSize(new Dimension(
					width * MapData.TILE_WIDTH + 3,
					height * MapData.TILE_HEIGHT + 3));
			
			this.addMouseListener(this);
    	}
    	
    	public void selectTile(int tile) {
    		this.tile = tile;
    		if ((tile < scroll.getValue() * height)
    				|| (tile > (scroll.getValue()+width+1) * height))
    			scroll.setValue(tile/height);
    		else
    			repaint();
    	}
    	
    	public int getSelectedTile() {
            return tile;
        }
    	
    	public JScrollBar getScrollBar() {
    		return scroll;
    	}
    	
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			if (isEnabled()) {
				drawTiles(g2d);
				drawGrid(g2d);
			} else {
				scroll.setEnabled(false);
			}
		}
		
		private void drawTiles(Graphics2D g) {
			int dtile;
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					dtile = (i+scroll.getValue())*height+j;
					if (dtile < 1024) {
						g.drawImage(
								MapDisplay.getTileImage(
										TileEditor.getDrawTilesetNumber(tilesetChooser.getSelectedIndex()),
										dtile,
										mapDisplay.getSelectedSectorPalNumber()),
								i*MapData.TILE_WIDTH + 1, j*MapData.TILE_HEIGHT + 1,
								MapData.TILE_WIDTH, MapData.TILE_HEIGHT,
								this);
						if (dtile == tile) {
							g.setPaint(Color.yellow);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
							g.fillRect(i*MapData.TILE_WIDTH + 1, j*MapData.TILE_HEIGHT + 1,
									MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
						}
					}
				}
			}
		}
		
		private void drawGrid(Graphics2D g) {
			g.setPaint(Color.black);
			// Draw vertical lines
			for (int i = 0; i < width+1; i++)
				g.drawLine(1 + i * MapData.TILE_WIDTH, 1, 1 + i * MapData.TILE_WIDTH, height * MapData.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < height+1; i++)
				g.drawLine(1, 1 + i * MapData.TILE_HEIGHT, width * MapData.TILE_WIDTH, 1 + i * MapData.TILE_HEIGHT);
			
			// Blank pixel in the bottom right corner
			g.drawLine(width * MapData.TILE_WIDTH + 1,
					height * MapData.TILE_HEIGHT + 1,
					width * MapData.TILE_WIDTH + 1,
					height * MapData.TILE_HEIGHT + 1);
			
			// Draw border
			g.setColor(Color.black);
			g.draw(new Rectangle2D.Double(0, 0,
					width * MapData.TILE_WIDTH + 2,
					height * MapData.TILE_HEIGHT + 2));
		}
		
		public void adjustmentValueChanged(AdjustmentEvent e) {
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			if ((e.getButton() == MouseEvent.BUTTON1) && isEnabled()) {
				tile = (((e.getX()-1) / MapData.TILE_WIDTH) + scroll.getValue()) * height
							+ ((e.getY()-1) / MapData.TILE_HEIGHT);
				repaint();
			}
		}

		public void mousePressed(MouseEvent e) { }

		public void mouseReleased(MouseEvent e) { }

		public void mouseEntered(MouseEvent e) { }

		public void mouseExited(MouseEvent e) { }
    }
	
	public static class MapData {
		public static final int WIDTH_IN_TILES = 32*8;
		public static final int HEIGHT_IN_TILES = 80*4;
		public static final int SECTOR_WIDTH = 8;
		public static final int SECTOR_HEIGHT = 4;
		public static final int WIDTH_IN_SECTORS = WIDTH_IN_TILES/SECTOR_WIDTH;
		public static final int HEIGHT_IN_SECTORS = HEIGHT_IN_TILES/SECTOR_HEIGHT;
		public static final int TILE_WIDTH = 32;
		public static final int TILE_HEIGHT = 32;
		
		public static final int NUM_MAP_TSETS = 32;
		public static final int NUM_DRAW_TSETS = 20;
		
		// Stores the map tiles
		private int[][] mapTiles;
		private Sector[][] sectors;
		private ArrayList<SpriteEntry>[][] spriteAreas;
		private NPC[] npcs;
		
		public MapData() {
			reset();
		}
		
		public void reset() {
			mapTiles = new int[HEIGHT_IN_TILES][WIDTH_IN_TILES];
			sectors = new Sector[HEIGHT_IN_SECTORS][WIDTH_IN_SECTORS];
			for (int i = 0; i < sectors.length; ++i)
				for (int j = 0; j < sectors[i].length; ++j)
					sectors[i][j] = new Sector();
			spriteAreas = new ArrayList[HEIGHT_IN_SECTORS/2][WIDTH_IN_SECTORS];
			for (int i = 0; i < spriteAreas.length; ++i)
				for (int j = 0; j < spriteAreas[i].length; ++j)
					spriteAreas[i][j] = new ArrayList<SpriteEntry>();
			npcs = new NPC[1584];
		}
		
		public void load(Project proj) {
			importMapTiles(new File(proj.getFilename("eb.MapModule", "map_tiles")));
			importSectors(new File(proj.getFilename("eb.MapModule", "map_sectors")));
			importSpritePlacements(new File(proj.getFilename("eb.MapSpriteModule", "map_sprites")));
			importNPCs(new File(proj.getFilename("eb.MiscTablesModule", "npc_config_table")));
		}
		
		public void save(Project proj) {
			exportMapTiles(new File(proj.getFilename("eb.MapModule", "map_tiles")));
			exportSectors(new File(proj.getFilename("eb.MapModule", "map_sectors")));
		}
		
		public NPC getNPC(int n) {
			return npcs[n];
		}
		
		public Sector getSector(int sectorX, int sectorY) {
			return sectors[sectorY][sectorX];
		}
		
		public List<SpriteEntry> getSpriteArea(int areaX, int areaY) {
			return spriteAreas[areaY][areaX];
		}
		
		private void importMapTiles(File f) {
	        if (f == null)
	            return;
	        
			try {
				FileInputStream in = new FileInputStream(f);
		        setMapTilesFromStream(in);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void exportMapTiles(File f) {
	        if (f == null)
	            return;
	        
			try {
				FileOutputStream out = new FileOutputStream(f);
		        writeMapTilesToStream(out);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void importNPCs(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<String, Object>> sectorsMap = (Map<Integer, Map<String, Object>>) yaml.load(input);
				
				NPC npc;
				for (Map.Entry<Integer, Map<String, Object>> entry: sectorsMap.entrySet()) {
					npc = new NPC((Integer) entry.getValue().get("Sprite"),
							(String) entry.getValue().get("Direction"));
					npcs[entry.getKey()] = npc;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void importSpritePlacements(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<Integer, List<Map<String, Integer>>>> spritesMap =
						(Map<Integer, Map<Integer, List<Map<String, Integer>>>>) yaml.load(input);
				int y, x;
				ArrayList<SpriteEntry> area;
				for (Map.Entry<Integer, Map<Integer, List<Map<String, Integer>>>> rowEntry: spritesMap.entrySet()) {
					y = rowEntry.getKey();
					for (Map.Entry<Integer, List<Map<String, Integer>>> entry: rowEntry.getValue().entrySet()) {
						x = entry.getKey();
						area = this.spriteAreas[y][x];
						area.clear();
						if (entry.getValue() == null)
							continue;
						
						for (Map<String, Integer> spe : entry.getValue()) {
							area.add(new SpriteEntry(
									spe.get("X"), spe.get("Y"), spe.get("NPC ID")));
						}
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void importSectors(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<String, Object>> sectorsMap = (Map<Integer, Map<String, Object>>) yaml.load(input);
				
				int y, x;
				Sector sec;
				for (Map.Entry<Integer, Map<String, Object>> entry: sectorsMap.entrySet()) {
					y = entry.getKey() / WIDTH_IN_SECTORS;
					x = entry.getKey() % WIDTH_IN_SECTORS;
					sec = sectors[y][x];
					sec.tileset = (Integer) (entry.getValue().get("Tileset"));
					sec.palette = (Integer) (entry.getValue().get("Palette"));
					sec.music = (Integer) (entry.getValue().get("Music"));
					sec.item = (Integer) (entry.getValue().get("Item"));
					sec.teleport = (String) (entry.getValue().get("Teleport"));
					sec.townmap = (String) (entry.getValue().get("Town Map"));
					sec.setting = (String) (entry.getValue().get("Setting"));
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void exportSectors(File f) {
			Map<Integer, Map<String, Object>> sectorsMap = new HashMap<Integer, Map<String, Object>>();
			int i = 0;
			for (Sector[] row: sectors) {
				for (Sector s: row) {
					Map<String, Object> entry = new HashMap<String, Object>();
					entry.put("Item", s.item);
					entry.put("Music", s.music);
					entry.put("Palette", s.palette);
					entry.put("Setting", s.setting);
					entry.put("Teleport", s.teleport);
					entry.put("Tileset", s.tileset);
					entry.put("Town Map", s.townmap);
					sectorsMap.put(i, entry);
					++i;
				}
			}
			
			try {
				FileWriter fw = new FileWriter(f);
				DumperOptions options = new DumperOptions();
			    options.setDefaultFlowStyle(FlowStyle.BLOCK);
			    Yaml yaml = new Yaml(options);
	        	yaml.dump(sectorsMap, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void setMapTilesFromStream(InputStream in) {
			String tmp;
			try {
				for (int i = 0; i < mapTiles.length; i++) {
					for (int j = 0; j < mapTiles[i].length; j++) {
						tmp = ""+((char) in.read());
						tmp += (char) in.read();
						tmp += (char) in.read();
						mapTiles[i][j] = Integer.parseInt(tmp, 16);
						in.read(); // " " or "\n"
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void writeMapTilesToStream(FileOutputStream out) {
			try {
				String tmp;
				for (int i = 0; i < mapTiles.length; i++) {
					for (int j = 0; j < mapTiles[i].length; j++) {
						tmp = ToolModule.addZeros(Integer.toHexString(mapTiles[i][j]), 3);
						out.write(tmp.charAt(0));
						out.write(tmp.charAt(1));
						out.write(tmp.charAt(2));
						if (j != mapTiles[i].length - 1)
							out.write(' ');
					}
					out.write('\n');
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public int getMapTile(int x, int y) {
			return mapTiles[y][x];
		}
		
		public void setMapTile(int x, int y, int tile) {
			mapTiles[y][x] = tile;
		}
		
		public static class Sector {
			public int tileset = 0, palette = 0, music = 0, item;
			public String townmap, setting, teleport;
			
			public void reset() {
				tileset = 0; palette = 0; music = 0; item = 0;
				townmap = "none"; setting = "none"; teleport = "disabled";
			}

			public void copy(Sector other) {
				try {
					this.tileset = other.tileset;
					this.palette = other.palette;
					this.music = other.music;
					this.item = other.item;
					this.townmap = other.townmap;
					this.setting = other.setting;
					this.teleport = other.teleport;
				} catch (Exception e) {
					
				}
			}
		}
		
		public static class SpriteEntry {
			public int x, y, npcID;
			public SpriteEntry(int x, int y, int npcID) {
				this.x = x; this.y = y; this.npcID = npcID;
			}
		}
		
		// Only store the info we need
		public static class NPC {
			public int sprite, direction;
			public NPC(int sprite, String direction) {
				this.sprite = sprite;
				direction = direction.toLowerCase();
				if (direction.equals("up"))
					this.direction = 0;
				else if (direction.equals("right"))
					this.direction = 1;
				else if (direction.equals("down"))
					this.direction = 2;
				else
					this.direction = 3;
			}
		}

		public void nullSpriteData() {
			// TODO Auto-generated method stub
			
		}

		public void nullMapData() {
			for (int i = 0; i < mapTiles.length; i++) {
				for (int j = 0; j < mapTiles[i].length; j++) {
					mapTiles[i][j] = 0;
				}
			}
			for (Sector[] row: sectors) {
				for (Sector s: row) {
					s.reset();
				}
			}
		}

		public void nullEnemyData() {
			// TODO Auto-generated method stub
			
		}

		public void nullDoorData() {
			// TODO Auto-generated method stub
			
		}
	}

	public void load(Project proj) {
		map.load(proj);
	}

	public void save(Project proj) {
		map.save(proj);
	}
	
	public void reset() {
		map.reset();
	}
	
	private void updateXYScrollBars() {
		xScroll.removeAdjustmentListener(this);
		xScroll.setValue(mapDisplay.getMapX());
		xScroll.addAdjustmentListener(this);
		yScroll.removeAdjustmentListener(this);
		yScroll.setValue(mapDisplay.getMapY());
		yScroll.addAdjustmentListener(this);
	}
	
	private void updateXYFields() {
		xField.getDocument().removeDocumentListener(this);
		xField.setText(Integer.toString(mapDisplay.getMapX()));
		xField.getDocument().addDocumentListener(this);
		yField.getDocument().removeDocumentListener(this);
		yField.setText(Integer.toString(mapDisplay.getMapY()));
		yField.getDocument().addDocumentListener(this);
	}
	
	// Returns true if the map palette is invalid for the new tileset
	private boolean updatePaletteChooser(int mapTset, int mapPal) {
		palChooser.removeActionListener(this);
		palChooser.removeAllItems();
		TileEditor.Tileset tileset = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(mapTset)];
		TileEditor.Tileset.Palette pal;
		for (int i = 0; i < tileset.getPaletteCount(); i++) {
			if ((pal = tileset.getPalette(i)).getMapTileset() == mapTset) {
				palChooser.addItem(Integer.toString(pal.getMapPalette()));
			}
		}
		if (mapPal >= palChooser.getItemCount()) {
			palChooser.setSelectedIndex(palChooser.getItemCount()-1);
			palChooser.addActionListener(this);
			return true;
		} else {
			palChooser.setSelectedIndex(mapPal);
			palChooser.addActionListener(this);
			return false;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("sectorChanged")) {
			MapData.Sector sect = mapDisplay.getSelectedSector();
			if (sect == null) {
				tilesetChooser.setEnabled(false);
				palChooser.setEnabled(false);
				musicChooser.setEnabled(false);
			} else {
				if (!tilesetChooser.isEnabled()) {
					tilesetChooser.setEnabled(true);
					palChooser.setEnabled(true);
					musicChooser.setEnabled(true);
				}
				if (tilesetChooser.getSelectedIndex() != sect.tileset) {
					updatePaletteChooser(sect.tileset, sect.palette);
					tilesetChooser.removeActionListener(this);
					tilesetChooser.setSelectedIndex(sect.tileset);
					tilesetChooser.addActionListener(this);
				} else if (palChooser.getSelectedIndex() != sect.palette) {
					updatePaletteChooser(sect.tileset, sect.palette);
					palChooser.removeActionListener(this);
					palChooser.setSelectedIndex(sect.palette);
					palChooser.addActionListener(this);
				}
				
				if (musicChooser.getSelectedIndex() != sect.music) {
					musicChooser.removeActionListener(this);
					musicChooser.setSelectedIndex(sect.music);
					musicChooser.addActionListener(this);
				}
				
				if (tileSelector != null)
					tileSelector.repaint();
			}
		} else if (e.getSource().equals(tilesetChooser)) {
			mapDisplay.setSelectedSectorTileset(tilesetChooser.getSelectedIndex());
			if (updatePaletteChooser(mapDisplay.getSelectedSector().tileset,
					mapDisplay.getSelectedSector().palette))
				mapDisplay.setSelectedSectorPalette(palChooser.getSelectedIndex());
			mapDisplay.repaint();
			tileSelector.repaint();
		} else if (e.getSource().equals(palChooser)) {
			mapDisplay.setSelectedSectorPalette(palChooser.getSelectedIndex());
			tileSelector.repaint();
		} else if (e.getSource().equals(musicChooser)) {
			mapDisplay.getSelectedSector().music = musicChooser.getSelectedIndex();
		} else if (e.getActionCommand().equals("apply")) {
        	// TODO
        } else if (e.getActionCommand().equals("close")) {
            hide();
        } else if (e.getActionCommand().equals("mode0")) {
			mapDisplay.changeMode(0);
		} else if (e.getActionCommand().equals("mode1")) {
			mapDisplay.changeMode(1);
		} else if (e.getActionCommand().equals("mode2")) {
			mapDisplay.changeMode(2);
		} else if (e.getActionCommand().equals("mode6")) {
			mapDisplay.changeMode(6);
		} else if (e.getActionCommand().equals("mode7")) {
			mapDisplay.changeMode(7);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("delAllSprites")) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"Are you sure you want to "
							+ "delete all of the sprites?",
					"Are you sure?", JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				map.nullSpriteData();
				mapDisplay.repaint();
			}
		} else if (e.getActionCommand().equals("delAllMap")) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"Are you sure you want to clear the map and sector data?",
					"Are you sure?", JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				map.nullMapData();
				mapDisplay.repaint();
			}
		} else if (e.getActionCommand().equals("delAllDoors")) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"Are you sure you want to delete the door placement data?",
					"Are you sure?", JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				map.nullDoorData();
				mapDisplay.repaint();
			}
		} else if (e.getActionCommand().equals("delAllEnemies")) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"Are you sure you want to clear the enemy data?",
					"Are you sure?", JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				map.nullEnemyData();
				mapDisplay.repaint();
			}
		} else if (e.getActionCommand().equals("resetTileImages")) {
			mapDisplay.resetTileImageCache();
			// TODO
			/*if (gfxcontrol.getModeProps()[1] >= 2) {
				mapDisplay.repaint();
				mapDisplay.getTileChooser().repaint();
			}*/
		} else if (e.getActionCommand().equals("grid")) {
			mapDisplay.toggleGrid();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("spriteboxes")) {
			mapDisplay.toggleSpriteBoxes();
		} else if (e.getActionCommand().equals("mapchanges")) {
			mapDisplay.toggleMapChanges();
		} else if (e.getActionCommand().equals("sectorEdit")) {
			//net.starmen.pkhack.JHack.main.showModule(
			//		MapSectorPropertiesEditor.class, gfxcontrol
			//				.getSectorxy());
		} else if (e.getActionCommand().equals("findSprite")) {
			String tpt = JOptionPane.showInputDialog(mainWindow,
					"Enter TPT entry to search for.", Integer
							.toHexString(0));
			int tptNum, yesno;
			try {
				tptNum = Integer.parseInt(tpt, 16);
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(mainWindow,
						"\"" + tpt + "\" is not a valid hexidecimal number.\n"
						+ "Search was aborted.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			/*if (tpt != null) {
				for (int i = 0; i < (MapData.HEIGHT_IN_SECTORS / 2)
						* MapData.WIDTH_IN_SECTORS; i++) {
					ArrayList sprites = map.getSpritesData(i);
					MapData.SpriteLocation spLoc;
					int areaY, areaX;
					for (int j = 0; j < sprites.size(); j++) {
						spLoc = (MapData.SpriteLocation) sprites.get(j);
						if (spLoc.getTpt() == tptNum) {
							areaY = i / MapEditor.widthInSectors;
							areaX = i - (areaY * MapEditor.widthInSectors);
							gfxcontrol
									.setMapXY(
											(areaX * MapEditor.sectorWidth)
													+ (spLoc.getX() / MapEditor.tileWidth),
											(areaY * MapEditor.sectorHeight * 2)
													+ (spLoc.getY() / MapEditor.tileHeight));
							yesno = JOptionPane
									.showConfirmDialog(
											mainWindow,
											"I found a sprite with that TPT entry. Do you want to find another?",
											"Continue Search?",
											JOptionPane.YES_NO_OPTION);
							if (yesno == JOptionPane.NO_OPTION)
								return;
						}
					}
				}
				JOptionPane.showMessageDialog(mainWindow,
						"Could not find a sprite entry using TPT entry 0x"
								+ tpt + ".");
			}*/
		} else if (e.getActionCommand().equals("copySector")) {
			pasteSector.setEnabled(true);

			int sectorX = mapDisplay.getSectorX();
			int sectorY = mapDisplay.getSectorY();
			for (int i = 0; i < copiedSectorTiles.length; i++)
				for (int j = 0; j < copiedSectorTiles[i].length; j++)
					copiedSectorTiles[i][j] = map.getMapTile(
							j + sectorX * 8, i + sectorY * 4);
			copiedSector = map.getSector(sectorX, sectorY);
		} else if (e.getActionCommand().equals("pasteSector")) {
			int sectorX = mapDisplay.getSectorX();
			int sectorY = mapDisplay.getSectorY();
			for (int i = 0; i < copiedSectorTiles.length; i++)
				for (int j = 0; j < copiedSectorTiles[i].length; j++) {
					map.setMapTile(sectorX * 8 + j,
							sectorY * 4 + i,
							copiedSectorTiles[i][j]);
				}
			map.getSector(sectorX, sectorY).copy(copiedSector);
			mapDisplay.repaint();
			//gfxcontrol.updateComponents();
		/*} else if (ac.equals(ENEMY_SPRITES)) {
			gfxcontrol.toggleEnemySprites();
		} else if (ac.equals(ENEMY_COLORS)) {
			gfxcontrol.toggleEnemyColors();
		} else if (ac.equals(EVENTPAL)) {
			gfxcontrol.toggleEventPalette();*/
		} else if (e.getActionCommand().equals("undoMap")) {
			mapDisplay.undoMapAction();
		}
	}

	public void changedUpdate(DocumentEvent e) {
		if ((e.getDocument().equals(xField.getDocument())
					|| e.getDocument().equals(yField.getDocument()))
				&& (yField.getText().length() > 0)
				&& (xField.getText().length() > 0)) {
			int newX = Integer.parseInt(xField.getText()),
				newY = Integer.parseInt(yField.getText());
			if (newX > MapData.WIDTH_IN_TILES - mapDisplay.getScreenWidth()) {
				newY = MapData.WIDTH_IN_TILES - mapDisplay.getScreenHeight();
			} else if (newY < 0) {
				newY = 0;
			}
			if (newY > MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight()) {
				newY = MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight();
			} else if (newY < 0) {
				newY = 0;
			}
			if ((newX != mapDisplay.getMapX()) || (newY != mapDisplay.getMapY())) {
				mapDisplay.setMapXY(newX, newY);
				updateXYScrollBars();
				mapDisplay.repaint();
			}
		}
	}

	public void insertUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void removeUpdate(DocumentEvent e) {
		changedUpdate(e);
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource().equals(xScroll) || e.getSource().equals(yScroll)) {
			mapDisplay.setMapXY(xScroll.getValue(), yScroll.getValue());
			updateXYFields();
			mapDisplay.repaint();
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		int y = mapDisplay.getMapY() + (e.getWheelRotation() * 3);
		if (y > MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight())
			y = MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight();
		else if (y < 0)
			y = 0;
		if (y != mapDisplay.getMapY()) {
			mapDisplay.setMapY(y);
			updateXYScrollBars();
			updateXYFields();
			mapDisplay.repaint();
		}
	}
}
