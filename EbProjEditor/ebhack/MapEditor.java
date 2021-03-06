package ebhack;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import ebhack.MapEditor.MapData.Sector;

public class MapEditor extends ToolModule implements ActionListener,
		DocumentListener, AdjustmentListener, MouseWheelListener,
		ComponentListener {
	private JTextField xField, yField;
	private JComboBox tilesetChooser, palChooser, musicChooser;
	private JScrollBar xScroll, yScroll;
	private JMenu modeMenu;
	private JMenuItem /* sectorProps, *//* findSprite, */copySector, pasteSector,
			undo, redo;

	public static MapData map;
	private MapDisplay mapDisplay;
	private TileSelector tileSelector;
	private JLabel statusLabel;

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
		mainWindow.addComponentListener(this);

		JMenuBar menuBar = new JMenuBar();
		ButtonGroup group = new ButtonGroup();
		JCheckBoxMenuItem checkBox;
		JRadioButtonMenuItem radioButton;
		JMenu menu;
		
		/*
		 * menu = new JMenu("File");
		 * menu.add(ToolModule.createJMenuItem("Apply Changes", 's',
		 * "control S", "apply", this));
		 * menu.add(ToolModule.createJMenuItem("Exit", 'x', "alt F4", "close",
		 * this)); menuBar.add(menu);
		 */

		menu = new JMenu("Edit");
		undo = ToolModule.createJMenuItem("Undo Tile Change", 'u', "control Z",
				"undoMap", this);
		undo.setEnabled(false);
		menu.add(undo);
		redo = ToolModule.createJMenuItem("Redo Tile Change", 'r', "control Y",
				"redoMap", this);
		redo.setEnabled(false);
		menu.add(redo);
		menu.add(new JSeparator());
		copySector = ToolModule.createJMenuItem("Copy Sector", 'c',
				"control C", "copySector", this);
		menu.add(copySector);
		pasteSector = ToolModule.createJMenuItem("Paste Sector", 'p',
				"control V", "pasteSector", this);
		menu.add(pasteSector);
		// sectorProps = ToolModule.createJMenuItem("Edit Sector's Properties",
		// 'r', null, "sectorEdit", this);
		// menu.add(sectorProps);
		menu.add(new JSeparator());
		menu.add(ToolModule.createJMenuItem("Clear Map", 'm', null,
				"delAllMap", this));
		menu.add(ToolModule.createJMenuItem("Delete All Sprites", 's', null,
				"delAllSprites", this));
		menu.add(ToolModule.createJMenuItem("Delete All Doors", 'o', null,
				"delAllDoors", this));
		menu.add(ToolModule.createJMenuItem("Clear Enemy Placements", 'e',
				null, "delAllEnemies", this));
		menu.add(ToolModule.createJMenuItem("Delete/Clear All Of The Above",
				'a', null, "delAllEverything", this));
		menu.add(new JSeparator());
		menu.add(ToolModule.createJMenuItem("Clear Tile Image Cache", 't',
				"control R", "resetTileImages", this));
		menu.add(new JSeparator());
		menu.add(ToolModule.createJMenuItem("Highlight Multiple Tiles", 'h', "control H",
				"highlightMultipleTilesExternal", this));
		menu.add(ToolModule.createJMenuItem("Select Area", 'a', "A",
				"selectAreaExternal", this));
		menu.add(ToolModule.createJMenuItem("Select Snakey Area", 's', "S",
				"selectSnakeyAreaExternal", this));
		menuBar.add(menu);

		modeMenu = new JMenu("Mode");
		group = new ButtonGroup();
		radioButton = new JRadioButtonMenuItem("Map Edit");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F1"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode0");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Sprite Edit");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F2"));
		radioButton.setSelected(false);
		radioButton.setActionCommand("mode1");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Door Edit");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F3"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode2");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Enemy Edit");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F4"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode7");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Hotspot Edit");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F5"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode6");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		radioButton = new JRadioButtonMenuItem("Whole View");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F6"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode8");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);
		menuBar.add(modeMenu);
		radioButton = new JRadioButtonMenuItem("Game View");
		radioButton.setAccelerator(KeyStroke.getKeyStroke("F7"));
		radioButton.setSelected(true);
		radioButton.setActionCommand("mode9");
		radioButton.addActionListener(this);
		group.add(radioButton);
		modeMenu.add(radioButton);

		menu = new JMenu("Options");
		checkBox = new JCheckBoxMenuItem("Show Grid");
        checkBox.setAccelerator( KeyStroke.getKeyStroke( "control G" ) );
		checkBox.setMnemonic('g');
		checkBox.setSelected(true);
		checkBox.setActionCommand("grid");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Enable Highlighting");
        checkBox.setAccelerator( KeyStroke.getKeyStroke( "control F" ) );
		checkBox.setMnemonic('h');
		checkBox.setSelected(false);
		checkBox.setActionCommand("toggleHighlighting");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Tile Numbers");
        checkBox.setAccelerator( KeyStroke.getKeyStroke( "control T" ) );
		checkBox.setMnemonic('t');
		checkBox.setSelected(false);
		checkBox.setActionCommand("tileNums");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Enable Tile Dragging");
        checkBox.setAccelerator( KeyStroke.getKeyStroke( "control D" ) );
		checkBox.setMnemonic('d');
		checkBox.setSelected(true);
		checkBox.setActionCommand("enableTileDragging");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show NPC IDs");
		checkBox.setMnemonic('n');
		checkBox.setSelected(true);
		checkBox.setActionCommand("npcNums");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Sprite Boxes");
		checkBox.setMnemonic('b');
		checkBox.setSelected(true);
		checkBox.setActionCommand("spriteboxes");
		checkBox.addActionListener(this);
		menu.add(checkBox);
		checkBox = new JCheckBoxMenuItem("Show Map Changes");
        checkBox.setAccelerator( KeyStroke.getKeyStroke( "control Q" ) );
		checkBox.setMnemonic('c');
		checkBox.setSelected(false);
		checkBox.setActionCommand("mapchanges");
		checkBox.addActionListener(this);
		// menu.add(checkBox);
		menu.add(new PrefsCheckBox("Mask Overscan in Preview", prefs,
				"maskOverscan", false, 'o'));
		menuBar.add(menu);

		mainWindow.setJMenuBar(menuBar);

		JPanel contentPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel("X: "));
		xField = ToolModule.createSizedJTextField(
				Integer.toString(MapData.WIDTH_IN_TILES).length(), true);
		xField.getDocument().addDocumentListener(this);
		panel.add(xField);
		panel.add(new JLabel("Y: "));
		yField = ToolModule.createSizedJTextField(
				Integer.toString(MapData.HEIGHT_IN_TILES).length(), true);
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

		panel.add( new JLabel( "Status: " ) );
		statusLabel = new JLabel( "OK" );
		panel.add( statusLabel );	
		
		contentPanel.add(panel, BorderLayout.NORTH);

//		JPanel statusPanel = new JPanel( new FlowLayout() );
//		statusPanel.add( new JLabel( "Status: " ) );
//		statusLabel = new JLabel( "OK" );
//		statusPanel.add( statusLabel );	
//		statusPanel.add( ToolModule.createSizedJTextField(Integer.toString(MapData.WIDTH_IN_TILES).length(), true) );
//		mainWindow.getContentPane().add( statusPanel, BorderLayout.SOUTH );

		tilesetChooser.setEnabled(false);
		musicChooser.setEnabled(false);

		mapDisplay = new MapDisplay(map, copySector, pasteSector, undo, redo,
				prefs, this);
		mapDisplay.addMouseWheelListener(this);
		mapDisplay.addActionListener(this);
		mapDisplay.init();
		contentPanel.add(mapDisplay, BorderLayout.CENTER);

		xScroll = new JScrollBar(JScrollBar.HORIZONTAL, 0,
				mapDisplay.getScreenWidth(), 0, MapData.WIDTH_IN_TILES);
		xScroll.addAdjustmentListener(this);
		contentPanel.add(xScroll, BorderLayout.SOUTH);
		yScroll = new JScrollBar(JScrollBar.VERTICAL, 0,
				mapDisplay.getScreenHeight(), 0, MapData.HEIGHT_IN_TILES);
		yScroll.addAdjustmentListener(this);
		contentPanel.add(yScroll, BorderLayout.EAST);

		mainWindow.getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		tileSelector = new TileSelector( 24, 4, mapDisplay );
		mapDisplay.setTileSelector(tileSelector);
		mainWindow.getContentPane().add(
				ToolModule.pairComponents(tileSelector,
						tileSelector.getScrollBar(), false),
				BorderLayout.PAGE_END);

		mainWindow.invalidate();
		mainWindow.pack();
		// mainWindow.setSize(300, 400);
		mainWindow.setLocationByPlatform(true);
		mainWindow.validate();
		// mainWindow.setResizable(false);
		mainWindow.setResizable(true);
	}

	private void loadTilesetNames() {
		tilesetChooser.removeActionListener(this);
		tilesetChooser.removeAllItems();
		for (int i = 0; i < MapData.NUM_MAP_TSETS; i++)
			tilesetChooser
					.addItem(getNumberedString(
							TileEditor.TILESET_NAMES[TileEditor
									.getDrawTilesetNumber(i)], i, false));
		tilesetChooser.addActionListener(this);
	}

	// TODO make it actually load names from musiclisting.txt
	public void loadMusicNames() {
		musicChooser.removeActionListener(this);
		musicChooser.removeAllItems();
		for (int i = 0; i < 164; i++)
			musicChooser.addItem(getNumberedString("", i, false));
		musicChooser.addActionListener(this);
	}

	public void show() {
		super.show();

		mainWindow.setVisible(true);
	}

	public void show(Object o) {
		super.show();
		if (o instanceof DoorEditor) {
			mapDisplay.seek((DoorEditor) o);
			mapDisplay.repaint();
		} else if (o instanceof int[]) {
			int[] coords = (int[]) o;
			mapDisplay.setMapXY(coords[0] / MapData.TILE_WIDTH, coords[1]
					/ MapData.TILE_HEIGHT);
			updateXYFields();
			updateXYScrollBars();
			mapDisplay.repaint();
		}
		mainWindow.setVisible(true);
	}

	public void hide() {
		if (isInited)
			mainWindow.setVisible(false);
	}

	public static class MapDisplay extends AbstractButton implements
			ActionListener, MouseListener, MouseMotionListener {
		private YMLPreferences prefs;
		private MapData map;
		private JMenuItem copySector, pasteSector, undoButton, redoButton;
		private MapEditor mapEditor;

		private final ActionEvent sectorEvent = new ActionEvent(this,
				ActionEvent.ACTION_PERFORMED, "sectorChanged");

		private static Image[][][] tileImageCache;
		
		private interface UndoableAction {
			public void redo( MapData map );
			public void undo( MapData map );
		}
		
		private class UndoableTileChange implements UndoableAction {
			public int x, y, oldTile, newTile;

			public UndoableTileChange( int x, int y, int oldTile, int newTile ) {
				this.x = x;
				this.y = y;
				this.oldTile = oldTile;
				this.newTile = newTile;
			}
			
			public void redo( MapData map ) {
				map.setMapTile( x, y, newTile );
			}
			
			public void undo( MapData map ) {
				map.setMapTile( x, y, oldTile );
			}
		}
		
		private class UndoablePaste implements UndoableAction {
			public int x, y;
			public int[][] oldTiles;
			public int[][] newTiles;
			
			public UndoablePaste( int _x, int _y, int[][] _oldTiles, int[][] _newTiles ) {
				x = _x;
				y = _y;
				oldTiles = _oldTiles;
				newTiles = _newTiles;
			}
			
			public void undo(MapData map) {
				for( int i = 0; i < oldTiles.length; i++ ) {
        			for( int j = 0; j < oldTiles[0].length; j++ ) {
        				map.setMapTile( x + i, y + j, oldTiles[i][j] );
        			}
        		}
			}
			
			public void redo(MapData map) {
				for( int i = 0; i < newTiles.length; i++ ) {
        			for( int j = 0; j < newTiles[0].length; j++ ) {
        				map.setMapTile( x + i, y + j, newTiles[i][j] );
        			}
        		}
			}
		}
		
		private class UndoableSnakeyPaste implements UndoableAction {
			public int x, y;//x, y of first point
			public List<Point> snake;//the points for the whole snake, relative to the first point
			public int[] oldTiles;
			public int[] newTiles;
			
			public UndoableSnakeyPaste( int _x, int _y, List<Point> _snake, int[] _oldTiles, int[] _newTiles ) {
				x = _x;
				y = _y;
				snake = _snake;
				oldTiles = _oldTiles;
				newTiles = _newTiles;
			}
			
			public void undo(MapData map) {
				for( int i = 0; i < snake.size(); i++ ) {
					Point point = snake.get( i );
    				map.setMapTile( x + point.x, y + point.y, oldTiles[i] );
        		}
			}
			
			public void redo(MapData map) {
				for( int i = 0; i < snake.size(); i++ ) {
					Point point = snake.get( i );
    				map.setMapTile( x + point.x, y + point.y, newTiles[i] );
        		}
			}
		}

//		private class UndoableSectorPaste implements UndoableAction {
//			public int sectorX, sectorY;
//			private int[][] tiles;
//			private Sector sector;
//
//			public UndoableSectorPaste(int sectorX, int sectorY, int[][] tiles,
//					Sector sector) {
//				this.sectorX = sectorX;
//				this.sectorY = sectorY;
//				this.tiles = tiles;
//				this.sector = sector;
//			}
//
//		}

		private Stack<UndoableAction> undoStack = new Stack<UndoableAction>();
		private Stack<UndoableAction> redoStack = new Stack<UndoableAction>();

		private int screenWidth = 24;
		private int screenHeight = 12;

		// Map X and Y coordinates of the tile displayed in the top left corner
		private int x = 0, y = 0;
		// Data for the selected sector
		private MapData.Sector selectedSector = null;
		private int sectorX, sectorY;
		private int sectorPal;
		private boolean grid = true;
		private boolean spriteBoxes = true;

		// Moving stuff
		private int movingDrawX, movingDrawY;
		private int movingNPC = -1;
		private Image movingNPCimg;
		private int[] movingNPCdim;
		private MapData.Door movingDoor = null;

		// Popup menus
		private int popupX, popupY;
		private JPopupMenu spritePopupMenu, doorPopupMenu;
		private JMenuItem delNPC, cutNPC, copyNPC, switchNPC;
		private int copiedNPC = 0;
		private MapData.SpriteEntry popupSE;
		private JMenuItem delDoor, cutDoor, copyDoor, editDoor;
		private MapData.Door popupDoor, copiedDoor;

		// Seeking stuff
		private int seekDrawX, seekDrawY;
		private DoorEditor doorSeeker;

		// Editing hotspot
		private MapData.Hotspot editHS = null;
		private int editHSx1, editHSy1;
		private int hsMouseX, hsMouseY;

		// Mode settings
		private int previousMode = 0;
		private int togglePreviousMode = -1;
		private boolean editMap = true, drawTileNums = false, enableHighlighting = false, enableDraggingTiles = true;
		private int dragTileX = -1, dragTileY = -1;
		private int selectAreaX1 = -1, selectAreaY1 = -1, selectAreaX2 = -1, selectAreaY2 = -1;
		private List<Point> selectSnake = null;
		private String subMode = null;
		private String dragType = null;
		private List<Integer> highlightedTiles = null;
		
		private boolean drawSprites = false, editSprites = false,
				drawSpriteNums = true;
		private boolean drawDoors = false, editDoors = false, seekDoor = false;
		private boolean drawEnemies = false, editEnemies = false;
		private boolean drawHotspots = false, editHotspots = false;
		private boolean gamePreview = false;
		private boolean tvPreview = false;
		private int tvPreviewX, tvPreviewY, tvPreviewW, tvPreviewH;

		// Cache enemy colors
		public static Color[] enemyColors = null;

		private TileSelector tileSelector;

		public MapDisplay( MapData map, JMenuItem copySector,
				JMenuItem pasteSector, JMenuItem undoButton,
				JMenuItem redoButton, YMLPreferences prefs, MapEditor mapEditor ) {
			super();
			
			this.mapEditor = mapEditor;

			if (enemyColors == null) {
				enemyColors = new Color[203];
				for (int i = 0; i < 203; ++i)
					enemyColors[i] = new Color(
							((int) (Math.E * 0x100000 * i)) & 0xffffff);
			}

			this.prefs = prefs;

			this.map = map;
			this.copySector = copySector;
			this.pasteSector = pasteSector;
			this.undoButton = undoButton;
			this.redoButton = redoButton;

			if (tileImageCache == null)
				resetTileImageCache();

			// Create Sprite popup menu
			spritePopupMenu = new JPopupMenu();
			spritePopupMenu.add(ToolModule.createJMenuItem("New NPC", 'n',
					null, "newNPC", this));
			spritePopupMenu.add(delNPC = ToolModule.createJMenuItem(
					"Delete NPC", 'd', null, "delNPC", this));
			spritePopupMenu.add(cutNPC = ToolModule.createJMenuItem("Cut NPC",
					'u', null, "cutNPC", this));
			spritePopupMenu.add(copyNPC = ToolModule.createJMenuItem(
					"Copy NPC", 'c', null, "copyNPC", this));
			spritePopupMenu.add(ToolModule.createJMenuItem("Paste NPC", 'p',
					null, "pasteNPC", this));
			spritePopupMenu.add(switchNPC = ToolModule.createJMenuItem(
					"Switch NPC", 'p', null, "switchNPC", this));

			// Create Door popup menu
			doorPopupMenu = new JPopupMenu();
			doorPopupMenu.add(ToolModule.createJMenuItem("New door", 'n', null,
					"newDoor", this));
			doorPopupMenu.add(delDoor = ToolModule.createJMenuItem(
					"Delete door", 'd', null, "delDoor", this));
			doorPopupMenu.add(cutDoor = ToolModule.createJMenuItem("Cut Door",
					'u', null, "cutDoor", this));
			doorPopupMenu.add(copyDoor = ToolModule.createJMenuItem(
					"Copy Door", 'c', null, "copyDoor", this));
			doorPopupMenu.add(ToolModule.createJMenuItem("Paste Door", 'p',
					null, "pasteDoor", this));
			doorPopupMenu.add(editDoor = ToolModule.createJMenuItem(
					"Edit door", 'e', null, "editDoor", this));

			addMouseListener(this);
			addMouseMotionListener(this);

			setPreferredSize(new Dimension(
					screenWidth * MapData.TILE_WIDTH + 2, screenHeight
							* MapData.TILE_HEIGHT + 2));
		}

		public void init() {
			selectSector(0, 0);
			changeMode(0);
			reset();
		}

		public void reset() {
			undoStack.clear();
			undoButton.setEnabled(false);
			redoStack.clear();
			redoButton.setEnabled(false);
		}

		public void setTileSelector(TileSelector tileSelector) {
			this.tileSelector = tileSelector;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			if (isEnabled())
				drawMap(g2d);
			else {
				// Draw border
				g2d.setColor(Color.black);
				g2d.draw(new Rectangle2D.Double(0, 0, screenWidth
						* MapData.TILE_WIDTH + 2, screenHeight
						* MapData.TILE_HEIGHT + 2));
			}
		}

		private void drawMap(Graphics2D g) {
			int i, j, a;
			g.setPaint(Color.white);
			g.setFont(new Font("Arial", Font.PLAIN, 12));

			MapData.Sector sector;
			int pal;
			for (i = 0; i < screenHeight; i++) {
				for (j = 0; j < screenWidth; j++) {
					int mapTile = map.getMapTile( x + j, y + i );
					sector = map.getSector((j + x) / MapData.SECTOR_WIDTH, (i + y) / MapData.SECTOR_HEIGHT);
					pal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(sector.tileset)].getPaletteNum(sector.tileset, sector.palette);
					
                    Image tileImage = getTileImage(TileEditor.getDrawTilesetNumber(sector.tileset), mapTile, pal);                    
                    g.drawImage( tileImage, 
                        j * MapData.TILE_WIDTH + 1, i * MapData.TILE_HEIGHT + 1,
                        MapData.TILE_WIDTH,         MapData.TILE_HEIGHT, this );
                    
					if (drawTileNums && !gamePreview) {
						drawNumber(g, mapTile, j * MapData.TILE_WIDTH + 1, i * MapData.TILE_HEIGHT + 1, 
								prefs.getValueAsBoolean( "useHexNumbers" ), false);
					}
					
                    if( editMap && enableHighlighting && mapTile == tileSelector.getSelectedTile() 
                    		&& sector.tileset == mapEditor.tilesetChooser.getSelectedIndex() ) {
						g.setPaint(Color.white);
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
						g.fillRect(j * MapData.TILE_HEIGHT + 1, i * MapData.TILE_WIDTH + 1, MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
                    } else if( editMap && ( enableHighlighting || ( subMode != null && subMode.equals( "highlightMultipleTiles" ) ) )
                    		&& highlightedTiles != null && highlightedTiles.contains( mapTile ) 
                    		&& sector.tileset == mapEditor.tilesetChooser.getSelectedIndex() ) {
                    	g.setPaint( colorFromInt( highlightedTiles.indexOf( mapTile ) ) );
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
						g.fillRect(j * MapData.TILE_HEIGHT + 1, i * MapData.TILE_WIDTH + 1, MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
                    }
				}
			}

			if (grid && !gamePreview)
				drawGrid(g);
			
			//draw selection rectangle
            if( editMap && subMode != null && subMode.equals( "selectArea" ) ) {
            	if( selectAreaX1 != -1 ) {
            		int rectX = selectAreaX1 > selectAreaX2 ? selectAreaX2 : selectAreaX1;
            		int rectY = selectAreaY1 > selectAreaY2 ? selectAreaY2 : selectAreaY1;

                	g.setPaint(Color.red);
                	g.draw( new Rectangle2D.Double(
					  ( rectX - x ) * MapData.TILE_WIDTH + 1, ( rectY - y ) * MapData.TILE_HEIGHT + 1, 
					  Math.abs( selectAreaX2 - selectAreaX1 ) * MapData.TILE_WIDTH + MapData.TILE_WIDTH,
					  Math.abs( selectAreaY2 - selectAreaY1 ) * MapData.TILE_HEIGHT + MapData.TILE_HEIGHT ) );
                	g.draw( new Rectangle2D.Double(
					  ( rectX - x ) * MapData.TILE_WIDTH, ( rectY - y ) * MapData.TILE_HEIGHT, 
					  Math.abs( selectAreaX2 - selectAreaX1 ) * MapData.TILE_WIDTH + MapData.TILE_WIDTH + 2,
					  Math.abs( selectAreaY2 - selectAreaY1 ) * MapData.TILE_HEIGHT + MapData.TILE_HEIGHT + 2 ) );
                	g.draw( new Rectangle2D.Double(
					  ( rectX - x ) * MapData.TILE_WIDTH + 2, ( rectY - y ) * MapData.TILE_HEIGHT + 2, 
					  Math.abs( selectAreaX2 - selectAreaX1 ) * MapData.TILE_WIDTH + MapData.TILE_WIDTH - 2,
					  Math.abs( selectAreaY2 - selectAreaY1 ) * MapData.TILE_HEIGHT + MapData.TILE_HEIGHT - 2 ) );
            	}
            }
			
			//draw snakey selection
            if( editMap && subMode != null && subMode.equals( "selectSnakeyArea" ) ) {
            	if( selectSnake != null && selectSnake.size() > 0 ) {
                	g.setPaint(Color.red);
                	
                	for( Point point: selectSnake ) {
                		Point point2 = new Point( selectAreaX1 + point.x - x, selectAreaY1 + point.y - y );
                		//left border
                		if( !selectSnake.contains( new Point( point.x - 1, point.y ) ) ) {
                    		g.draw( new Line2D.Double( point2.x * 32, point2.y * 32, point2.x * 32, ( point2.y + 1 ) * 32 + 2 ) );
                    		g.draw( new Line2D.Double( point2.x * 32 + 1, point2.y * 32, point2.x * 32 + 1, ( point2.y + 1 ) * 32 + 2 ) );
                    		g.draw( new Line2D.Double( point2.x * 32 + 2, point2.y * 32, point2.x * 32 + 2, ( point2.y + 1 ) * 32 + 2 ) );
                    	}
                		//top border
                		if( !selectSnake.contains( new Point( point.x, point.y - 1 ) ) ) {
                    		g.draw( new Line2D.Double( point2.x * 32, point2.y * 32, ( point2.x + 1 ) * 32, point2.y * 32 ) );
                    		g.draw( new Line2D.Double( point2.x * 32, point2.y * 32 + 1, ( point2.x + 1 ) * 32, point2.y * 32 + 1 ) );
                    		g.draw( new Line2D.Double( point2.x * 32, point2.y * 32 + 2, ( point2.x + 1 ) * 32, point2.y * 32 + 2 ) );
                    	}
                		//right border
                		if( !selectSnake.contains( new Point( point.x + 1, point.y ) ) ) {
                    		g.draw( new Line2D.Double( ( point2.x + 1 ) * 32, point2.y * 32, ( point2.x + 1 ) * 32, ( point2.y + 1 ) * 32 ) );
                    		g.draw( new Line2D.Double( ( point2.x + 1 ) * 32 + 1, point2.y * 32, ( point2.x + 1 ) * 32 + 1, ( point2.y + 1 ) * 32 ) );
                    		g.draw( new Line2D.Double( ( point2.x + 1 ) * 32 + 2, point2.y * 32, ( point2.x + 1 ) * 32 + 2, ( point2.y + 1 ) * 32 ) );
                    	}
                		//bottom border
                		if( !selectSnake.contains( new Point( point.x, point.y + 1 ) ) ) {
                    		g.draw( new Line2D.Double( point2.x * 32, ( point2.y + 1 ) * 32, ( point2.x + 1 ) * 32 + 2, ( point2.y + 1 ) * 32 ) );
                    		g.draw( new Line2D.Double( point2.x * 32, ( point2.y + 1 ) * 32 + 1, ( point2.x + 1 ) * 32 + 2, ( point2.y + 1 ) * 32 + 1 ) );
                    		g.draw( new Line2D.Double( point2.x * 32, ( point2.y + 1 ) * 32 + 2, ( point2.x + 1 ) * 32 + 2, ( point2.y + 1 ) * 32 + 2 ) );
                    	}
                	}
            	}
            }

			if (editMap && (selectedSector != null)) {
				int sXt, sYt;
				if (((sXt = sectorX * MapData.SECTOR_WIDTH)
						+ MapData.SECTOR_WIDTH >= x)
						&& (sXt < x + screenWidth)
						&& ((sYt = sectorY * MapData.SECTOR_HEIGHT)
								+ MapData.SECTOR_HEIGHT >= y)
						&& (sYt < y + screenHeight)) {
					g.setPaint(Color.yellow);
					g.draw(new Rectangle2D.Double(
					  (sXt - x) * MapData.TILE_WIDTH + 1, (sYt - y) * MapData.TILE_HEIGHT + 1, 
					  MapData.SECTOR_WIDTH * MapData.TILE_WIDTH, MapData.SECTOR_HEIGHT* MapData.TILE_HEIGHT));
				}
			}

			// Draw border
			g.setColor(Color.black);
			g.draw(new Rectangle2D.Double(0, 0, screenWidth
					* MapData.TILE_WIDTH + 2, screenHeight
					* MapData.TILE_HEIGHT + 2));

			if (drawSprites) {
				MapData.NPC npc;
				int[] wh;
				List<MapData.SpriteEntry> area;
				for (i = y & (~7); i < (y & (~7)) + screenHeight + 8; i += 8) {
					for (j = x & (~7); j < (x & (~7)) + screenWidth + 8; j += 8) {
						try {
							area = map.getSpriteArea(j >> 3, i >> 3);
							for (MapData.SpriteEntry e : area) {
								npc = map.getNPC(e.npcID);
								wh = map.getSpriteWH(npc.sprite);
								if (spriteBoxes && !gamePreview) {
									g.setPaint(Color.RED);
									g.draw(new Rectangle2D.Double(e.x + (j - x)
											* MapData.TILE_WIDTH - wh[0] / 2,
											e.y + (i - y) * MapData.TILE_HEIGHT
													- wh[1] + 8, wh[0] + 1,
											wh[1] + 1));
								}
								g.drawImage(map.getSpriteImage(npc.sprite,
										npc.direction), e.x + (j - x)
										* MapData.TILE_WIDTH - wh[0] / 2 + 1,
										e.y + (i - y) * MapData.TILE_HEIGHT
												- wh[1] + 9, this);
								if (drawSpriteNums && !gamePreview) {
									drawNumber(g, e.npcID, e.x + (j - x)
											* MapData.TILE_WIDTH - wh[0] / 2,
											e.y + (i - y) * MapData.TILE_HEIGHT
													- wh[1] + 8, prefs.getValueAsBoolean( "useHexNumbers" ), true);
								}
							}
						} catch (Exception e) {

						}
					}
				}

				if (editSprites && (movingNPC != -1)) {
					if (spriteBoxes) {
						g.setPaint(Color.RED);
						g.draw(new Rectangle2D.Double(movingDrawX - 1,
								movingDrawY - 1, movingNPCdim[0] + 1,
								movingNPCdim[1] + 1));
					}
					g.drawImage(movingNPCimg, movingDrawX, movingDrawY, this);
				}
			}

			if (drawDoors) {
				List<MapData.Door> area;
				for (i = y & (~7); i < (y & (~7)) + screenHeight + 8; i += 8) {
					for (j = x & (~7); j < (x & (~7)) + screenWidth + 8; j += 8) {
						try {
							area = map.getDoorArea(j >> 3, i >> 3);
							for (MapData.Door e : area) {
								g.setPaint(Color.WHITE);
								g.draw(new Rectangle2D.Double(e.x * 8 + (j - x)
										* MapData.TILE_WIDTH + 1, e.y * 8
										+ (i - y) * MapData.TILE_HEIGHT + 1, 8,
										8));
								g.draw(new Rectangle2D.Double(e.x * 8 + (j - x)
										* MapData.TILE_WIDTH + 3, e.y * 8
										+ (i - y) * MapData.TILE_HEIGHT + 3, 4,
										4));
								g.setPaint(Color.BLUE);
								g.draw(new Rectangle2D.Double(e.x * 8 + (j - x)
										* MapData.TILE_WIDTH + 2, e.y * 8
										+ (i - y) * MapData.TILE_HEIGHT + 2, 6,
										6));
							}
						} catch (Exception e) {

						}
					}
				}

				if (editDoors && (movingDoor != null)) {
					g.setPaint(Color.WHITE);
					g.draw(new Rectangle2D.Double(movingDrawX + 1,
							movingDrawY + 1, 8, 8));
					g.draw(new Rectangle2D.Double(movingDrawX + 3,
							movingDrawY + 3, 4, 4));
					g.setPaint(Color.BLUE);
					g.draw(new Rectangle2D.Double(movingDrawX + 2,
							movingDrawY + 2, 6, 6));
				}

				if (seekDoor) {
					g.setPaint(Color.WHITE);
					g.draw(new Rectangle2D.Double(seekDrawX + 1, seekDrawY + 1,
							8, 8));
					g.draw(new Rectangle2D.Double(seekDrawX + 3, seekDrawY + 3,
							4, 4));
					g.setPaint(Color.MAGENTA);
					g.draw(new Rectangle2D.Double(seekDrawX + 2, seekDrawY + 2,
							6, 6));
				}
			}

			if (drawEnemies) {
				g.setFont(new Font("Arial", Font.PLAIN, 12));
				for (i = -(y % 2); i < screenHeight; i += 2) {
					for (j = -(x % 2); j < screenWidth; j += 2) {
						a = map.getMapEnemyGroup((x + j) / 2, (y + i) / 2);
						if (a != 0) {
							g.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.5F));
							g.setPaint(enemyColors[a]);
							g.fill(new Rectangle2D.Double(j
									* MapData.TILE_WIDTH + 1, i
									* MapData.TILE_HEIGHT + 1,
									MapData.TILE_WIDTH * 2,
									MapData.TILE_HEIGHT * 2));

							g.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 1.0F));
							/*
							 * g.setPaint(Color.black); message =
							 * addZeros(Integer.toString(a), 3); rect =
							 * g.getFontMetrics().getStringBounds(message, g);
							 * rect.setRect( j * MapData.TILE_WIDTH + 1, i *
							 * MapData.TILE_HEIGHT + 1, rect.getWidth(),
							 * rect.getHeight()); g.fill(rect);
							 * g.setPaint(Color.white); g.drawString(message,
							 * (float) (j * MapData.TILE_WIDTH + 1), (float) (i
							 * * MapData.TILE_HEIGHT + rect.getHeight()));
							 */
							drawNumber(g, a, j * MapData.TILE_WIDTH + 1, i
									* MapData.TILE_HEIGHT + 1, prefs.getValueAsBoolean( "useHexNumbers" ), false);
						}
					}
				}
			}

			if (drawHotspots) {
				MapData.Hotspot hs;
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.8F));
				int tx1, ty1, tx2, ty2;
				for (i = 0; i < map.numHotspots(); ++i) {
					hs = map.getHotspot(i);
					if (hs == editHS)
						continue;
					tx1 = hs.x1 / 4 - x;
					ty1 = hs.y1 / 4 - y;
					tx2 = hs.x2 / 4 - x;
					ty2 = hs.y2 / 4 - y;
					if (((tx1 >= 0) && (tx1 <= screenWidth) && (ty1 >= 0) && (ty1 <= screenHeight))
							|| ((tx2 >= 0) && (tx2 <= screenWidth)
									&& (ty2 >= 0) && (ty2 <= screenHeight))) {
						g.setPaint(Color.PINK);
						g.fill(new Rectangle2D.Double(hs.x1 * 8 - x
								* MapData.TILE_WIDTH + 1, hs.y1 * 8 - y
								* MapData.TILE_HEIGHT + 1, (hs.x2 - hs.x1) * 8,
								(hs.y2 - hs.y1) * 8));
						drawNumber(g, i,
								hs.x1 * 8 - x * MapData.TILE_WIDTH + 1, hs.y1
										* 8 - y * MapData.TILE_HEIGHT + 1,
								prefs.getValueAsBoolean( "useHexNumbers" ), false);
					}
				}

				if (editHotspots && (editHS != null)) {
					g.setPaint(Color.WHITE);
					if (editHSx1 != -1) {
						tx1 = editHSx1 * 8 - x * MapData.TILE_WIDTH + 1;
						ty1 = editHSy1 * 8 - y * MapData.TILE_HEIGHT + 1;
						g.fill(new Rectangle2D.Double(tx1, ty1, hsMouseX - tx1,
								hsMouseY - ty1));
					} else {
						g.fill(new Rectangle2D.Double(hsMouseX + 1,
								hsMouseY + 1, 65, 65));
					}
				}
			}

			if (gamePreview && tvPreview) {
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 1.0F));
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, tvPreviewX - tvPreviewW, screenHeight
						* MapData.TILE_HEIGHT);
				g.fillRect(tvPreviewX + tvPreviewW, 0,
						(screenWidth * MapData.TILE_WIDTH) - tvPreviewX
								- tvPreviewW, screenHeight
								* MapData.TILE_HEIGHT);
				g.fillRect(0, 0, screenWidth * MapData.TILE_WIDTH, tvPreviewY
						- tvPreviewH);
				g.fillRect(0, tvPreviewY + tvPreviewH, screenWidth
						* MapData.TILE_WIDTH,
						(screenHeight * MapData.TILE_HEIGHT) - tvPreviewY
								- tvPreviewH);

				// hardcoded for sprite of size 16,24
				g.drawImage(map.getSpriteImage(1, 2), tvPreviewX - 7,
						tvPreviewY - 15, this);
			}
		}

		private Rectangle2D textBG;

		private void drawNumber(Graphics2D g, int n, int x, int y, boolean hex,
				boolean above) {
			String s;
			if (hex)
				s = addZeros(Integer.toHexString(n), 4);
			else
				s = addZeros(Integer.toString(n), 3);

			if (textBG == null)
				textBG = g.getFontMetrics().getStringBounds(s, g);

			g.setPaint(Color.black);
			if (above) {
				textBG.setRect(x, y - textBG.getHeight(), textBG.getWidth(),
						textBG.getHeight());
				g.fill(textBG);
				g.setPaint(Color.white);
				g.drawString(s, x, y);
			} else {
				textBG.setRect(x, y, textBG.getWidth(), textBG.getHeight());
				g.fill(textBG);
				g.setPaint(Color.white);
				g.drawString(s, x, y + ((int) textBG.getHeight()));
			}
		}

		private void drawGrid(Graphics2D g) {
			g.setPaint(Color.black);
			// Draw vertical lines
			for (int i = 0; i < screenWidth + 1; i++)
				g.drawLine(1 + i * MapData.TILE_WIDTH, 1, 1 + i
						* MapData.TILE_WIDTH, screenHeight
						* MapData.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < screenHeight + 1; i++)
				g.drawLine(1, 1 + i * MapData.TILE_HEIGHT, screenWidth
						* MapData.TILE_WIDTH, 1 + i * MapData.TILE_HEIGHT);

			// Blank pixel in the bottom right corner
			g.drawLine(screenWidth * MapData.TILE_WIDTH + 1, screenHeight
					* MapData.TILE_HEIGHT + 1, screenWidth * MapData.TILE_WIDTH
					+ 1, screenHeight * MapData.TILE_HEIGHT + 1);
		}

		public static Image getTileImage(int loadtset, int loadtile,
				int loadpalette) {
			if (tileImageCache[loadtset][loadtile][loadpalette] == null) {
				tileImageCache[loadtset][loadtile][loadpalette] = TileEditor.tilesets[loadtset]
						.getArrangementImage(loadtile, loadpalette);
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
			sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)].
							getPaletteNum(selectedSector.tileset,selectedSector.palette);
		}

		public void setSelectedSectorPalette(int pal) {
			selectedSector.palette = pal;
			sectorPal = TileEditor.tilesets[TileEditor
					.getDrawTilesetNumber(selectedSector.tileset)]
					.getPaletteNum(selectedSector.tileset,
							selectedSector.palette);
		}

		public int getSelectedSectorPalNumber() {
			return sectorPal;
		}

		public void setMapXY(int x, int y) {
			x = Math.max(0, x);
			y = Math.max(0, y);
			this.x = Math.min(x, MapData.WIDTH_IN_TILES - screenWidth);
			this.y = Math.min(y, MapData.HEIGHT_IN_TILES - screenHeight);
		}

		public void setMapX(int x) {
			setMapXY(x, y);
		}

		public void setMapY(int y) {
			setMapXY(x, y);
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
				sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)]
						.getPaletteNum(selectedSector.tileset, selectedSector.palette);
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

		// Sprites
		private MapData.SpriteEntry getSpriteEntryFromMouseXY(int mouseX,
				int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			return map.getSpriteEntryFromCoords(areaX, areaY, mouseX, mouseY);
		}

		private int popNpcIdFromMouseXY(int mouseX, int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			return map.popNPCFromCoords(areaX, areaY, mouseX, mouseY);
		}

		private void pushNpcIdFromMouseXY(int npc, int mouseX, int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			map.pushNPCFromCoords(npc, areaX, areaY, mouseX, mouseY);
		}

		// Doors
		private MapData.Door getDoorFromMouseXY(int mouseX, int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			return map.getDoorFromCoords(areaX, areaY, mouseX / 8, mouseY / 8);
		}

		private MapData.Door popDoorFromMouseXY(int mouseX, int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			return map.popDoorFromCoords(areaX, areaY, mouseX / 8, mouseY / 8);
		}

		private void pushDoorFromMouseXY(MapData.Door door, int mouseX,
				int mouseY) {
			int areaX = (x + mouseX / MapData.TILE_WIDTH) / 8, areaY = (y + mouseY
					/ MapData.TILE_HEIGHT) / 8;
			mouseX += (x % 8) * MapData.TILE_WIDTH;
			mouseX %= (MapData.TILE_WIDTH * 8);
			mouseY += (y % 8) * MapData.TILE_HEIGHT;
			mouseY %= (MapData.TILE_HEIGHT * 8);
			door.x = mouseX / 8;
			door.y = mouseY / 8;
			map.pushDoorFromCoords(door, areaX, areaY);
		}

		private static final Cursor blankCursor = Toolkit.getDefaultToolkit()
				.createCustomCursor(
						new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
						new Point(0, 0), "blank cursor");

		public void actionPerformed(ActionEvent ae) {
			if (ae.getActionCommand().equals("newNPC")) {
				pushNpcIdFromMouseXY(0, popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("delNPC")) {
				popNpcIdFromMouseXY(popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("cutNPC")) {
				copiedNPC = popNpcIdFromMouseXY(popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("copyNPC")) {
				copiedNPC = popupSE.npcID;
			} else if (ae.getActionCommand().equals("pasteNPC")) {
				pushNpcIdFromMouseXY(copiedNPC, popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("switchNPC")) {
				String input = JOptionPane.showInputDialog(this,
						"Switch this to a different NPC", popupSE.npcID);
				if (input != null) {
					popupSE.npcID = Integer.parseInt(input);
					repaint();
				}
			} else if (ae.getActionCommand().equals("newDoor")) {
				pushDoorFromMouseXY(new MapData.Door(), popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("delDoor")) {
				popDoorFromMouseXY(popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("cutDoor")) {
				copiedDoor = popDoorFromMouseXY(popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("copyDoor")) {
				copiedDoor = popupDoor.copy();
			} else if (ae.getActionCommand().equals("pasteDoor")) {
				pushDoorFromMouseXY(copiedDoor.copy(), popupX, popupY);
				repaint();
			} else if (ae.getActionCommand().equals("editDoor")) {
				ebhack.Ebhack.main.showModule(DoorEditor.class, popupDoor);
			}
		}


		public void externalCommand(String cmd) {
			if( cmd.equals( "highlightMultipleTiles" ) ) {			
				if( this.subMode != null && this.subMode.equals( "highlightMultipleTiles" ) ) {
					this.subMode = null;
					mapEditor.statusLabel.setText( "Exited Highlight Tiles Mode" );
				} else {
					clearSubmodes();
					this.subMode = "highlightMultipleTiles";
					this.highlightedTiles = null;	
					mapEditor.statusLabel.setText( "Entered Highlight Tiles Mode" );
				}
				tileSelector.repaint();
			} else if( cmd.equals( "selectArea" ) ) {
				if( this.subMode != null && this.subMode.equals( "selectArea" ) ) {
					this.subMode = null;
					mapEditor.statusLabel.setText( "Exited Select Area Mode" );

					this.setCursor( Cursor.getDefaultCursor() );
				} else {
					mapEditor.statusLabel.setText( "Entered Select Area Mode" );
					clearSubmodes();
					this.subMode = "selectArea";
					this.selectAreaX1 = -1;
					this.selectAreaY1 = -1;
					this.selectAreaX2 = -1;
					this.selectAreaY2 = -1;
					
					this.setCursor( new Cursor( Cursor.CROSSHAIR_CURSOR ) );
				}
			} else if( cmd.equals( "selectSnakeyArea" ) ) {
				if( this.subMode != null && this.subMode.equals( "selectSnakeyArea" ) ) {
					this.subMode = null;
					mapEditor.statusLabel.setText( "Exited Select Snakey Area Mode" );

					this.setCursor( Cursor.getDefaultCursor() );
				} else {
					mapEditor.statusLabel.setText( "Entered Select Snakey Area Mode" );
					clearSubmodes();
					this.subMode = "selectSnakeyArea";
					this.selectAreaX1 = -1;
					this.selectAreaY1 = -1;
					
					this.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
				}
			}
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			// Make sure they didn't click on the border
			if ((e.getX() >= 1)
					&& (e.getX() <= screenWidth * MapData.TILE_WIDTH + 2)
					&& (e.getY() >= 1)
					&& (e.getY() <= screenHeight * MapData.TILE_HEIGHT + 2)) {
				if (editMap) {
					if( subMode == null ) {
						//select sector
						if (e.getButton() == MouseEvent.BUTTON3) {
							// Make sure they didn't click on the border
							int sX = (x + ((e.getX() - 1) / MapData.TILE_WIDTH))
									/ MapData.SECTOR_WIDTH;
							int sY = (y + ((e.getY() - 1) / MapData.TILE_HEIGHT))
									/ MapData.SECTOR_HEIGHT;
							selectSector(sX, sY);
							
							dragType = null;
						}
					} else if( subMode.equals( "highlightMultipleTiles" ) ) {
						int mX = ( e.getX() - 1 ) / MapData.TILE_WIDTH + x;
						int mY = ( e.getY() - 1 ) / MapData.TILE_HEIGHT + y;
						if( this.highlightedTiles == null ) {
							this.highlightedTiles = new ArrayList<Integer>();
						}
						int mapTile = map.getMapTile( mX, mY );
						if( !highlightedTiles.contains( mapTile ) ) {
							highlightedTiles.add( new Integer( map.getMapTile( mX, mY ) ) );
						}
						repaint();
						tileSelector.repaint();
					} 
				} else if (editSprites) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						popupX = e.getX();
						popupY = e.getY();
						popupSE = getSpriteEntryFromMouseXY(e.getX(), e.getY());
						if (popupSE == null) {
							delNPC.setEnabled(false);
							cutNPC.setEnabled(false);
							copyNPC.setEnabled(false);
							switchNPC.setText("Switch NPC");
							switchNPC.setEnabled(false);
						} else {
							delNPC.setEnabled(true);
							cutNPC.setEnabled(true);
							copyNPC.setEnabled(true);
							switchNPC.setText("Switch NPC (" + popupSE.npcID
									+ ")");
							switchNPC.setEnabled(true);
						}
						spritePopupMenu.show(this, e.getX(), e.getY());
					}
				} else if (editDoors) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						popupX = e.getX();
						popupY = e.getY();
						popupDoor = getDoorFromMouseXY(e.getX(), e.getY());
						if (popupDoor == null) {
							delDoor.setEnabled(false);
							cutDoor.setEnabled(false);
							copyDoor.setEnabled(false);
							editDoor.setEnabled(false);
						} else {
							delDoor.setEnabled(true);
							cutDoor.setEnabled(true);
							copyDoor.setEnabled(true);
							editDoor.setEnabled(true);
						}
						doorPopupMenu.show(this, e.getX(), e.getY());
					}
				} else if (seekDoor) {
					doorSeeker.seek(x * 4 + seekDrawX / 8, y * 4 + seekDrawY
							/ 8);
					doorSeeker = null;
					changeMode(previousMode);
					repaint();
				} else if (editEnemies) {
					int eX = ((e.getX() - 1) / MapData.TILE_WIDTH + x) / 2;
					int eY = ((e.getY() - 1) / MapData.TILE_HEIGHT + y) / 2;
					if (e.isShiftDown()) {
						tileSelector.selectTile(map.getMapEnemyGroup(eX, eY));
					} else {
						map.setMapEnemyGroup(eX, eY,
								tileSelector.getSelectedTile());
						repaint();
					}
				} else if (editHotspots) {
					int mx = ((e.getX() - 1) / 8) + (x * 4), my = ((e.getY() - 1) / 8)
							+ (y * 4);
					if (editHS != null) {
						if (editHSx1 == -1) {
							editHSx1 = mx;
							editHSy1 = my;
							repaint();
						} else {
							editHS.x1 = editHSx1;
							editHS.y1 = editHSy1;
							editHS.x2 = mx;
							editHS.y2 = my;
							editHS = null;
							repaint();
						}
					} else {
						for (int i = 0; i < map.numHotspots(); ++i) {
							MapData.Hotspot hs = map.getHotspot(i);
							if ((mx >= hs.x1) && (mx <= hs.x2) && (my >= hs.y1)
									&& (my <= hs.y2)) {
								editHS = hs;
								editHSx1 = editHSy1 = -1;
								hsMouseX = e.getX() & (~7);
								hsMouseY = e.getY() & (~7);
								repaint();
								return;
							}
						}
					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			//TV Preview
			if (e.isControlDown() && (e.getButton() == MouseEvent.BUTTON1)) {
				if (togglePreviousMode == -1) {
					togglePreviousMode = previousMode;
					changeMode(9);

					tvPreview = true;
					tvPreviewX = e.getX();
					tvPreviewY = e.getY();

					tvPreviewH = 224 / 2;
					if (prefs.getValueAsBoolean("maskOverscan")) {
						tvPreviewW = 240 / 2;
					} else {
						tvPreviewW = 256 / 2;
					}

					this.setCursor(blankCursor);
					repaint();
				}
			//Dragging a sprite/door 
			//OR: "Dragging" a tile (paintbrush-like behavior)
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				if (editSprites && (movingNPC == -1)) {
					movingNPC = popNpcIdFromMouseXY(mx, my);
					if (movingNPC != -1) {
						MapData.NPC tmp = map.getNPC(movingNPC);
						movingNPCimg = map.getSpriteImage(tmp.sprite,
								tmp.direction);
						movingNPCdim = map.getSpriteWH(tmp.sprite);
						movingDrawX = mx - movingNPCdim[0] / 2 + 1;
						movingDrawY = my - movingNPCdim[1] + 9;
						repaint();
					}
				} else if (editDoors && (movingDoor == null)) {
					movingDoor = popDoorFromMouseXY(mx, my);
					if (movingDoor != null) {
						movingDrawX = mx & (~7);
						movingDrawY = my & (~7);
						repaint();
					}
				} else if( (e.getX() >= 1)
						&& (e.getX() <= screenWidth * MapData.TILE_WIDTH + 2)
						&& (e.getY() >= 1)
						&& (e.getY() <= screenHeight * MapData.TILE_HEIGHT + 2)
						&& editMap ) {
					int mX = (e.getX() - 1) / MapData.TILE_WIDTH + x;
					int mY = (e.getY() - 1) / MapData.TILE_HEIGHT + y;
					if( subMode == null ) {
						if( e.getButton() == MouseEvent.BUTTON1 && enableDraggingTiles ) {
							if (e.isShiftDown()) {
								tileSelector.selectTile(map.getMapTile(mX, mY));
							} else if( e.isControlDown() ) {							
							} else {
								int oldTile = map.getMapTile( mX, mY );
								int newTile = tileSelector.getSelectedTile();
								
								dragType = "dragTiles";
								
								if( oldTile != newTile ) {
									// Keep track of the undo stuff
									undoStack.push( new UndoableTileChange( mX, mY, oldTile, newTile ) );
									undoButton.setEnabled( true );
									redoStack.clear();
		
									map.setMapTile( mX, mY, tileSelector.getSelectedTile() );
									repaint();
								}
								
								dragTileX = mX;
								dragTileY = mY;
							}
						}
					} else if( subMode.equals( "selectArea" ) ) {
						dragType = "select";
						
						selectAreaX1 = selectAreaX2 = mX;
						selectAreaY1 = selectAreaY2 = mY;
						
						repaint();
					} else if( subMode.equals( "selectSnakeyArea" ) ) {
						dragType = "selectSnake";
						
						selectAreaX1 = mX;
						selectAreaY1 = mY;
						selectSnake = new ArrayList<Point>();
						selectSnake.add( new Point( 0, 0 ) );//selectSnake's points are relative to the first
						
						selectAreaX2 = mX;
						selectAreaY2 = mY;//last point selected, for efficiency purposes
						
						repaint();
					}
				}
			}  else if( e.getButton() == MouseEvent.BUTTON3 ) {
				/**TODO: Select multiple sectors?*/
				if( subMode == null ) {
				}
				//Paste Area
				else if( subMode.equals( "selectArea" ) ) {
					dragType = "paste";
					
					int mX = (e.getX() - 1) / MapData.TILE_WIDTH + x;
					int mY = (e.getY() - 1) / MapData.TILE_HEIGHT + y;
					
					//copy tiles from selection
            		int rectX = selectAreaX1 > selectAreaX2 ? selectAreaX2 : selectAreaX1;
            		int rectY = selectAreaY1 > selectAreaY2 ? selectAreaY2 : selectAreaY1;
            		int rectWidth  = Math.abs( selectAreaX2 - selectAreaX1 ) + 1;
            		int rectHeight = Math.abs( selectAreaY2 - selectAreaY1 ) + 1;
            		
            		//save old tiles (for undo)
            		int[][] oldTiles = new int[rectWidth][rectHeight];
            		for( int i = 0; i < rectWidth; i++ ) {
            			for( int j = 0; j < rectHeight; j++ ) {
            				oldTiles[i][j] = map.getMapTile( mX + i, mY + j );
            			}
            		}
            		
            		//read the new tiles 
            		int[][] newTiles = new int[rectWidth][rectHeight];            	
            		for( int i = 0; i < rectWidth; i++ ) {
            			for( int j = 0; j < rectHeight; j++ ) {
            				newTiles[i][j] = map.getMapTile( rectX + i, rectY + j );
            			}
            		}

        			//set the new tiles (broken up in case the regions overlap)
            		for( int i = 0; i < rectWidth; i++ ) {
            			for( int j = 0; j < rectHeight; j++ ) {
            				map.setMapTile( mX + i, mY + j, newTiles[i][j] );
            			}
            		}
            		
            		undoStack.push( new UndoablePaste( mX, mY, oldTiles, newTiles ) );
					undoButton.setEnabled( true );
					redoStack.clear();
					repaint();
				} else if( subMode.equals( "selectSnakeyArea" ) ) {
					dragType = "snakeyPaste";
					
					int mX = (e.getX() - 1) / MapData.TILE_WIDTH + x;
					int mY = (e.getY() - 1) / MapData.TILE_HEIGHT + y;
					
					selectAreaX2 = mX;
					selectAreaY2 = mY;//for efficiency - only move pastes if one is different
					
					//save old tiles (for undo), read new tiles
            		int[] oldTiles = new int[selectSnake.size()];
            		int[] fromTiles = new int[selectSnake.size()];
            		for( int i = 0; i < oldTiles.length; i++ ) {
        				oldTiles[i] = map.getMapTile( mX + selectSnake.get( i ).x,  mY + selectSnake.get( i ).y );
            			fromTiles[i] = map.getMapTile( selectAreaX1 + selectSnake.get( i ).x, selectAreaY1 + selectSnake.get( i ).y );
            		}
            		
            		UndoableSnakeyPaste undo = new UndoableSnakeyPaste( mX, mY, selectSnake, oldTiles, fromTiles );
            		undo.redo( map );//cheating by using the redo code to set the tiles
            		
            		undoStack.push( undo );
					undoButton.setEnabled( true );
					redoStack.clear();
					repaint();
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			int mx = e.getX(), my = e.getY();
			if (e.getButton() == 1) {
				if (togglePreviousMode != -1) {
					changeMode(togglePreviousMode);
					togglePreviousMode = -1;
					this.setCursor(Cursor.getDefaultCursor());
					tvPreview = false;
					repaint();
				} else if (editSprites && (movingNPC != -1)) {
					pushNpcIdFromMouseXY(movingNPC, mx, my);
					movingNPC = -1;
					repaint();
				} else if (editDoors && (movingDoor != null)) {
					pushDoorFromMouseXY(movingDoor, mx, my);
					movingDoor = null;
					repaint();
				} else if( editMap && subMode == null ) {
					dragTileX = dragTileY = -1;
					mapEditor.statusLabel.setText( "OK" );
				} else if( editMap && subMode.equals( "selectArea" ) ) {
					if( dragType == null ) {
						mapEditor.statusLabel.setText( "dragType is null wtf" );
					}
					if( dragType.equals( "select" ) ) {
						mapEditor.statusLabel.setText( "Select finished" );
						dragType = null;
					}
				}
			} else if( e.getButton() == 3 ) {
				if( dragType == null ) {
				}
				//Paste
				else if( dragType.equals( "paste" ) ) {
					mapEditor.statusLabel.setText( "Paste finished" );
					dragType = null;
				}
				//Paste snake
				else if( dragType.equals( "snakeyPaste" ) ) {
					mapEditor.statusLabel.setText( "Snakey Paste finished" );
					dragType = null;
				}
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		public void mouseDragged(MouseEvent e) {
			if (tvPreview) {
				tvPreviewX = e.getX();
				tvPreviewY = e.getY();
				repaint();
			} else if (movingNPC != -1) {
				movingDrawX = e.getX() - movingNPCdim[0] / 2 + 1;
				movingDrawY = e.getY() - movingNPCdim[1] + 9;
				repaint();
			} else if (movingDoor != null) {
				movingDrawX = e.getX() & (~7);
				movingDrawY = e.getY() & (~7);
				repaint();
			} else if( editMap ) {
				int mX = (e.getX() - 1) / MapData.TILE_WIDTH + x;
				int mY = (e.getY() - 1) / MapData.TILE_HEIGHT + y;
				
				if( subMode == null ) { 
					mapEditor.statusLabel.setText( "dragType: " + dragType );
					//Pencil-dragging tiles
					if( dragType != null && dragType.equals( "dragTiles" ) ) {
						if( enableDraggingTiles ) {
							mapEditor.statusLabel.setText( "dragging tiles" );
							if( e.isControlDown() ) {							
							} else if( mX != dragTileX || mY != dragTileY ) {
								// Keep track of the undo stuff
								undoStack.push( new UndoableTileChange( mX, mY, 
										map.getMapTile( mX, mY ), tileSelector.getSelectedTile() ) );
								undoButton.setEnabled( true );
								redoStack.clear();
		
								map.setMapTile( mX, mY, tileSelector.getSelectedTile() );
								repaint();
								
								dragTileX = mX;
								dragTileY = mY;
							}
						}
					}
				} else if( subMode.equals( "selectArea" ) ) {
					if( dragType == null ) {
						mapEditor.statusLabel.setText( "null dragType in selectArea mode... ???" );
					}
					
					//Select Area
					if( dragType.equals( "select" ) ) {
						mapEditor.statusLabel.setText( "Selecting Area" );
						
						selectAreaX2 = mX;
						selectAreaY2 = mY;
						
						repaint();
					}
					//Paste
					else if( dragType.equals( "paste" ) ) {
						UndoablePaste undo = (UndoablePaste)undoStack.lastElement();
						
						if( undo.x != mX || undo.y != mY ) {
							mapEditor.statusLabel.setText( "Pasting/Moving Paste" );
							
							undoStack.pop();
							undo.undo( map );
							
							int rectWidth = undo.oldTiles.length;
							int rectHeight = undo.oldTiles[0].length;
							
							//save old tiles (for undo)
		            		int[][] oldTiles = new int[rectWidth][rectHeight];
		            		for( int i = 0; i < rectWidth; i++ ) {
		            			for( int j = 0; j < rectHeight; j++ ) {
		            				oldTiles[i][j] = map.getMapTile( mX + i, mY + j );
		            			}
		            		}
							
							undo.x = mX;
							undo.y = mY;
							undo.oldTiles = oldTiles;
							
							undo.redo( map );
							undoStack.push( undo );
							
							repaint();
						}
					}
				} else if( subMode.equals( "selectSnakeyArea" ) ) {
					if( dragType == null ) {
						mapEditor.statusLabel.setText( "How did dragType get to be null at such a time?" );
					}
					//Select Snakey Area
					else if( dragType.equals( "selectSnake" ) ) {
						if( mX != selectAreaX2 || mY != selectAreaY2 ) {
							mapEditor.statusLabel.setText( "Selecting Snakey Area" );
							
							Point point = new Point( mX - selectAreaX1, mY - selectAreaY1 );
							if( !selectSnake.contains( point ) ) {
								selectSnake.add( point );
								mapEditor.statusLabel.setText( "Point been added!!" );
							}
							
							selectAreaX2 = mX;
							selectAreaY2 = mY;
						}
					}
					//Snakey Paste
					else if( dragType.equals( "snakeyPaste" ) ) {
						if( mX != selectAreaX2 || mY != selectAreaY2 ) {
							mapEditor.statusLabel.setText( "Moving Snakey Paste" );
							
							UndoableSnakeyPaste undo = (UndoableSnakeyPaste)undoStack.pop();
							undo.undo( map );
							
							//save old tiles (for undo)
		            		int[] oldTiles = new int[undo.oldTiles.length];
		            		for( int i = 0; i < oldTiles.length; i++ ) {
		        				oldTiles[i] = map.getMapTile( mX + selectSnake.get( i ).x, mY + selectSnake.get( i ).y );
		            		}
							
							undo.x = mX;
							undo.y = mY;
							undo.oldTiles = oldTiles;
							
							undo.redo( map );
							undoStack.push( undo );
							
							selectAreaX2 = mX;
							selectAreaY2 = mY;
						}
					}
					repaint();
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			if (seekDoor) {
				seekDrawX = e.getX() & (~7);
				seekDrawY = e.getY() & (~7);
				repaint();
			} else if (editHotspots && (editHS != null)) {
				hsMouseX = e.getX() & (~7);
				hsMouseY = e.getY() & (~7);
				repaint();
			}
		}

		//called when submode is changed
		public void clearSubmodes() {
			subMode = null;
		}
		
		public void changeMode(int mode) {
			gamePreview = mode == 9;
			if (mode == 0) {
				previousMode = mode;
				// Map Mode
				editMap = true;
				drawSprites = false;
				editSprites = false;
				drawDoors = false;
				editDoors = false;
				seekDoor = false;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = false;
				editHotspots = false;
			} else if (mode == 1) {
				previousMode = mode;
				// Sprite Mode
				editMap = false;
				drawSprites = true;
				editSprites = true;
				drawDoors = false;
				editDoors = false;
				seekDoor = false;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = false;
				editHotspots = false;
			} else if (mode == 2) {
				previousMode = mode;
				// Door Mode
				editMap = false;
				drawSprites = false;
				editSprites = false;
				drawDoors = true;
				editDoors = true;
				seekDoor = false;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = false;
				editHotspots = false;
			} else if (mode == 4) {
				// Seek Door Mode
				editMap = false;
				drawSprites = true;
				editSprites = false;
				drawDoors = true;
				editDoors = false;
				seekDoor = true;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = false;
				editHotspots = false;
			} else if (mode == 6) {
				previousMode = mode;
				// Hotspot Mode
				editMap = false;
				drawSprites = false;
				editSprites = false;
				drawDoors = false;
				editDoors = false;
				seekDoor = false;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = true;
				editHotspots = true;
			} else if (mode == 7) {
				previousMode = mode;
				// Enemy Mode
				editMap = false;
				drawSprites = false;
				editSprites = false;
				drawDoors = false;
				editDoors = false;
				seekDoor = false;
				drawEnemies = true;
				editEnemies = true;
				drawHotspots = false;
				editHotspots = false;
			} else if (mode == 8) {
				previousMode = mode;
				// View All
				editMap = false;
				drawSprites = true;
				editSprites = false;
				drawDoors = true;
				editDoors = false;
				seekDoor = false;
				drawEnemies = true;
				editEnemies = false;
				drawHotspots = true;
				editHotspots = false;
			} else if (mode == 9) {
				previousMode = mode;
				// Preview
				editMap = false;
				drawSprites = true;
				editSprites = false;
				drawDoors = false;
				editDoors = false;
				seekDoor = false;
				drawEnemies = false;
				editEnemies = false;
				drawHotspots = false;
				editHotspots = false;
			}
		}

		public void seek(DoorEditor de) {
			changeMode(4);
			doorSeeker = de;
		}

		public void toggleGrid() {
			grid = !grid;
		}

		public void toggleSpriteBoxes() {
			spriteBoxes = !spriteBoxes;
		}

		public void toggleTileNums() {
			drawTileNums = !drawTileNums;
		}

		public void toggleTileDragging() {
			enableDraggingTiles = !enableDraggingTiles;
		}

		public void toggleHighlighting() {
			enableHighlighting = !enableHighlighting;
		}

		public void toggleSpriteNums() {
			drawSpriteNums = !drawSpriteNums;
		}

		public void toggleMapChanges() {
			// TODO Auto-generated method stub

		}

		public boolean undoMapAction() {
			if (!undoStack.empty()) {
				UndoableAction undo = undoStack.pop();
				undo.undo( map );
				if (undoStack.isEmpty())
					undoButton.setEnabled(false);
				redoStack.push(undo);
				redoButton.setEnabled(true);
				repaint();
				return true;
			} else
				return false;
		}

		public boolean redoMapAction() {
			if (!redoStack.empty()) {
				UndoableAction redo = redoStack.pop();
				redo.redo( map );
				if (redoStack.isEmpty())
					redoButton.setEnabled(false);
				undoStack.push(redo);
				undoButton.setEnabled(true);
				repaint();
				return true;
			} else
				return false;
		}

		public void setScreenSize(int newSW, int newSH) {
			if ((newSW != screenWidth) || (newSH != screenHeight)) {
				screenWidth = newSW;
				screenHeight = newSH;

				setMapXY(x, y);

				setPreferredSize(new Dimension(screenWidth * MapData.TILE_WIDTH
						+ 2, screenHeight * MapData.TILE_HEIGHT + 2));

				repaint();
			}
		}

		public void pasteSector(Sector copiedSector, int sectorX2,
				int sectorY2, int[][] copiedSectorTiles) {
			for (int i = 0; i < copiedSectorTiles.length; i++)
				for (int j = 0; j < copiedSectorTiles[i].length; j++) {
					map.setMapTile(sectorX * 8 + j, sectorY * 4 + i,
							copiedSectorTiles[i][j]);
				}
			map.getSector(sectorX, sectorY).copy(copiedSector);
			// TODO
			// undoStack.push(new UndoableSectorPaste(sectorX, sectorY,
			// copiedSectorTiles, copiedSector));
		}
	}

	private class TileSelector extends AbstractButton implements MouseListener,
			AdjustmentListener {
		private int width, height;
		private int tile = 0, mode = 0;
		private JScrollBar scroll;
		private MapDisplay mapDisplay;

		public TileSelector(int width, int height, MapDisplay mapDisplay) {
			super();

			this.width = width;
			this.height = height;
			this.mapDisplay = mapDisplay;

			scroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, width, 0,
					(1024 / height) + (1024 % height > 0 ? 1 : 0));
			scroll.addAdjustmentListener(this);

			setPreferredSize(new Dimension(width * MapData.TILE_WIDTH + 3,
					height * MapData.TILE_HEIGHT + 3));

			changeMode(0);

			this.addMouseListener(this);
		}

		public void setScreenSize(int newSW) {
			if (newSW != width) {
				width = newSW;

				setPreferredSize(new Dimension(width * MapData.TILE_WIDTH + 3,
						height * MapData.TILE_HEIGHT + 3));
				scroll.setVisibleAmount(width);
				if (scroll.getValue() + width + 1 > scroll.getMaximum()) {
					scroll.setValue(scroll.getMaximum() - width - 1);

				}

				repaint();
			}
		}

		public void changeMode(int mode) {
			this.mode = mode;
			tile = 0;
			if (mode == 7) {
				scroll.setEnabled(true);
				scroll.setMaximum(203 / (height) + 1);
				tile = Math.min(202, tile);
			} else if (mode == 0) {
				scroll.setEnabled(true);
				scroll.setMaximum(1024 / (height) + 1);
			} else {
				scroll.setEnabled(false);
				scroll.setMaximum(0);
			}
		}

		public void selectTile(int tile) {
			this.tile = tile;
			if ((tile < scroll.getValue() * height)
					|| (tile > (scroll.getValue() + width + 1) * height)) {
				scroll.setValue(tile / height);
			}
			else {
				repaint();
			}
			mapDisplay.repaint();
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
				if (mode == 0)
					drawTiles(g2d);
				else if (mode == 7)
					drawEnemies(g2d);
				drawGrid(g2d);
			} else {
				scroll.setEnabled(false);
			}
		}

		private void drawEnemies(Graphics2D g) {
			int dtile;
			String message;
			Rectangle2D rect;
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					dtile = (i + scroll.getValue()) * height + j;
					if (dtile < 203) {
						g.setPaint(MapDisplay.enemyColors[dtile]);
						g.fill(new Rectangle2D.Double(i * MapData.TILE_WIDTH
								+ 1, j * MapData.TILE_HEIGHT + 1,
								MapData.TILE_WIDTH, MapData.TILE_HEIGHT));

						g.setPaint(Color.black);
						message = addZeros(Integer.toString(dtile), 3);
						rect = g.getFontMetrics().getStringBounds(message, g);
						rect.setRect(i * MapData.TILE_WIDTH + 1, j
								* MapData.TILE_HEIGHT + 1, rect.getWidth(),
								rect.getHeight());
						g.fill(rect);
						g.setPaint(Color.white);
						g.drawString(message,
								(float) (i * MapData.TILE_WIDTH + 1),
								(float) (j * MapData.TILE_HEIGHT + rect
										.getHeight()));
						if (dtile == tile) {
							g.setPaint(Color.yellow);
							g.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 0.6F));
							g.fillRect(i * MapData.TILE_WIDTH + 1, j
									* MapData.TILE_HEIGHT + 1,
									MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
							g.setComposite(AlphaComposite.getInstance(
									AlphaComposite.SRC_OVER, 1.0F));
						}
					}
				}
			}
		}

		private void drawTiles(Graphics2D g) {
			int dtile;
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					dtile = (i + scroll.getValue()) * height + j;
					if (dtile < 1024) {
                        Image tileImage = MapDisplay.getTileImage(TileEditor.getDrawTilesetNumber(tilesetChooser.getSelectedIndex()), dtile, mapDisplay.getSelectedSectorPalNumber());
						g.drawImage( tileImage, 
                                i * MapData.TILE_WIDTH + 1, 
                                j * MapData.TILE_HEIGHT + 1, 
                                MapData.TILE_WIDTH,
								MapData.TILE_HEIGHT, 
                                this );
						if (dtile == tile) {
							g.setPaint(Color.white);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
							g.fillRect(i * MapData.TILE_WIDTH + 1, j * MapData.TILE_HEIGHT + 1,
									MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
						} else if( mapDisplay.highlightedTiles != null && mapDisplay.highlightedTiles.contains( dtile ) ) {
	                    	g.setPaint( colorFromInt( mapDisplay.highlightedTiles.indexOf( dtile ) ) );
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
							g.fillRect(i * MapData.TILE_WIDTH + 1, j * MapData.TILE_HEIGHT + 1, MapData.TILE_WIDTH, MapData.TILE_HEIGHT);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
	                    }
					}
				}
			}
		}

		private void drawGrid(Graphics2D g) {
			g.setPaint(Color.black);
			// Draw vertical lines
			for (int i = 0; i < width + 1; i++)
				g.drawLine(1 + i * MapData.TILE_WIDTH, 1, 1 + i
						* MapData.TILE_WIDTH, height * MapData.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < height + 1; i++)
				g.drawLine(1, 1 + i * MapData.TILE_HEIGHT, width
						* MapData.TILE_WIDTH, 1 + i * MapData.TILE_HEIGHT);

			// Blank pixel in the bottom right corner
			g.drawLine(width * MapData.TILE_WIDTH + 1, height
					* MapData.TILE_HEIGHT + 1, width * MapData.TILE_WIDTH + 1,
					height * MapData.TILE_HEIGHT + 1);

			// Draw border
			g.setColor(Color.black);
			g.draw(new Rectangle2D.Double(0, 0, width * MapData.TILE_WIDTH + 2,
					height * MapData.TILE_HEIGHT + 2));
		}

		public void adjustmentValueChanged(AdjustmentEvent e) {
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			if ((e.getButton() == MouseEvent.BUTTON1) && isEnabled()) {
				tile = (((e.getX() - 1) / MapData.TILE_WIDTH) + scroll
						.getValue())
						* height
						+ ((e.getY() - 1) / MapData.TILE_HEIGHT);
				if (mode == 0)
					tile = Math.min(tile, 1023);
				else if (mode == 7)
					tile = Math.min(tile, 202);
				repaint();
				mapDisplay.repaint();
				if (e.isShiftDown()) {
					ebhack.Ebhack.main.showModule(
                        TileEditor.class,
                        new int[] {
                                TileEditor.getDrawTilesetNumber(tilesetChooser.getSelectedIndex()),
                                mapDisplay.getSelectedSectorPalNumber(),
                                tile } );
				}
			}
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}
    
	public static class MapData {
		public static final int WIDTH_IN_TILES = 32 * 8;
		public static final int HEIGHT_IN_TILES = 80 * 4;
		public static final int SECTOR_WIDTH = 8;
		public static final int SECTOR_HEIGHT = 4;
		public static final int WIDTH_IN_SECTORS = WIDTH_IN_TILES
				/ SECTOR_WIDTH;
		public static final int HEIGHT_IN_SECTORS = HEIGHT_IN_TILES
				/ SECTOR_HEIGHT;
		public static final int TILE_WIDTH = 32;
		public static final int TILE_HEIGHT = 32;

		public static final int NUM_MAP_TSETS = 32;
		public static final int NUM_DRAW_TSETS = 20;

		// Stores the map tiles
		private int[][] mapTiles;
		private Sector[][] sectors;
		private ArrayList<SpriteEntry>[][] spriteAreas;
		private ArrayList<Door>[][] doorAreas;
		private NPC[] npcs;
		private static Image[][] spriteGroups;
		private static int[][] spriteGroupDims;
		private int[][] enemyPlacement;
		private Hotspot[] hotspots;

		public MapData() {
			reset();
		}

		public void reset() {
			mapTiles = new int[HEIGHT_IN_TILES][WIDTH_IN_TILES];
			sectors = new Sector[HEIGHT_IN_SECTORS][WIDTH_IN_SECTORS];
			for (int i = 0; i < sectors.length; ++i)
				for (int j = 0; j < sectors[i].length; ++j)
					sectors[i][j] = new Sector();
			spriteAreas = new ArrayList[HEIGHT_IN_SECTORS / 2][WIDTH_IN_SECTORS];
			for (int i = 0; i < spriteAreas.length; ++i)
				for (int j = 0; j < spriteAreas[i].length; ++j)
					spriteAreas[i][j] = new ArrayList<SpriteEntry>();
			doorAreas = new ArrayList[HEIGHT_IN_SECTORS / 2][WIDTH_IN_SECTORS];
			for (int i = 0; i < doorAreas.length; ++i)
				for (int j = 0; j < doorAreas[i].length; ++j)
					doorAreas[i][j] = new ArrayList<Door>();
			npcs = new NPC[1584];
			spriteGroups = new Image[464][4];
			spriteGroupDims = new int[464][2];
			enemyPlacement = new int[HEIGHT_IN_TILES / 2][WIDTH_IN_TILES / 2];
			hotspots = new Hotspot[56];
			for (int i = 0; i < hotspots.length; ++i)
				hotspots[i] = new Hotspot();
		}

		public void load(Project proj) {
			importMapTiles(new File(proj.getFilename("eb.MapModule",
					"map_tiles")));
			importSectors(new File(proj.getFilename("eb.MapModule",
					"map_sectors")));
			importSpritePlacements(new File(proj.getFilename(
					"eb.MapSpriteModule", "map_sprites")));
			importDoors(new File(proj.getFilename("eb.DoorModule", "map_doors")));
			importEnemyPlacement(new File(proj.getFilename("eb.MapEnemyModule",
					"map_enemy_placement")));
			importHotspots(new File(proj.getFilename("eb.MiscTablesModule",
					"map_hotspots")));

			loadExtraResources(proj);
		}

		public void loadExtraResources(Project proj) {
			importNPCs(new File(proj.getFilename("eb.MiscTablesModule",
					"npc_config_table")));
			importSpriteGroups(proj);
		}

		public void save(Project proj) {
			exportMapTiles(new File(proj.getFilename("eb.MapModule",
					"map_tiles")));
			exportSectors(new File(proj.getFilename("eb.MapModule",
					"map_sectors")));
			exportSpritePlacements(new File(proj.getFilename(
					"eb.MapSpriteModule", "map_sprites")));
			exportDoors(new File(proj.getFilename("eb.DoorModule", "map_doors")));
			exportEnemyPlacement(new File(proj.getFilename("eb.MapEnemyModule",
					"map_enemy_placement")));
			exportHotspots(new File(proj.getFilename("eb.MiscTablesModule",
					"map_hotspots")));
		}

		public NPC getNPC(int n) {
			return npcs[n];
		}

		public int[] getSpriteWH(int n) {
			return spriteGroupDims[n];
		}

		// Sprite Editing

		public SpriteEntry getSpriteEntryFromCoords(int areaX, int areaY,
				int x, int y) {
			int[] wh;
			NPC npc;
			for (SpriteEntry e : spriteAreas[areaY][areaX]) {
				npc = npcs[e.npcID];
				wh = spriteGroupDims[npc.sprite];
				if ((e.x >= x - wh[0] / 2) && (e.x <= x + wh[0] / 2)
						&& (e.y >= y - wh[1] / 2) && (e.y <= y + wh[1] / 2)) {
					return e;
				}
			}
			return null;
		}

		public int popNPCFromCoords(int areaX, int areaY, int x, int y) {
			int[] wh;
			NPC npc;
			for (SpriteEntry e : spriteAreas[areaY][areaX]) {
				npc = npcs[e.npcID];
				wh = spriteGroupDims[npc.sprite];
				if ((e.x >= x - wh[0] / 2) && (e.x <= x + wh[0] / 2)
						&& (e.y >= y - wh[1] / 2) && (e.y <= y + wh[1] / 2)) {
					spriteAreas[areaY][areaX].remove(e);
					return e.npcID;
				}
			}
			return -1;
		}

		public void pushNPCFromCoords(int npcid, int areaX, int areaY, int x,
				int y) {
			if ((areaX >= 0) && (areaY >= 0))
				spriteAreas[areaY][areaX].add(new SpriteEntry(x, y, npcid));
		}

		public List<SpriteEntry> getSpriteArea(int areaX, int areaY) {
			return spriteAreas[areaY][areaX];
		}

		// Door Editing

		public List<Door> getDoorArea(int areaX, int areaY) {
			return doorAreas[areaY][areaX];
		}

		public Door getDoorFromCoords(int areaX, int areaY, int x, int y) {
			for (Door e : doorAreas[areaY][areaX]) {
				if ((x <= e.x + 1) && (x >= e.x) && (y <= e.y + 1)
						&& (y >= e.y)) {
					return e;
				}
			}
			return null;
		}

		public Door popDoorFromCoords(int areaX, int areaY, int x, int y) {
			for (Door e : doorAreas[areaY][areaX]) {
				if ((x <= e.x + 1) && (x >= e.x) && (y <= e.y + 1)
						&& (y >= e.y)) {
					doorAreas[areaY][areaX].remove(e);
					return e;
				}
			}
			return null;
		}

		public void pushDoorFromCoords(Door door, int areaX, int areaY) {
			if ((areaX >= 0) && (areaY >= 0))
				doorAreas[areaY][areaX].add(door);
		}

		// Enemy Editing

		public int getMapEnemyGroup(int x, int y) {
			return enemyPlacement[y][x];
		}

		public void setMapEnemyGroup(int x, int y, int val) {
			enemyPlacement[y][x] = val;
		}

		// Hotspot

		public int numHotspots() {
			return 56;
		}

		public Hotspot getHotspot(int n) {
			return hotspots[n];
		}

		// Other

		public Sector getSector(int sectorX, int sectorY) {
			return sectors[sectorY][sectorX];
		}

		public Image getSpriteImage(int sprite, int direction) {
			return spriteGroups[sprite][direction];
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
				Map<Integer, Map<String, Object>> sectorsMap = (Map<Integer, Map<String, Object>>) yaml
						.load(input);

				NPC npc;
				for (Map.Entry<Integer, Map<String, Object>> entry : sectorsMap
						.entrySet()) {
					npc = new NPC((Integer) entry.getValue().get("Sprite"),
							(String) entry.getValue().get("Direction"));
					npcs[entry.getKey()] = npc;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void importSpriteGroups(Project proj) {
			int w, h, x, y, z;
			for (int i = 0; i < spriteGroups.length; ++i) {
				spriteGroups[i] = new Image[4];
				try {
					BufferedImage sheet = ImageIO.read(new File(proj
							.getFilename(
									"eb.SpriteGroupModule",
									"SpriteGroups/"
											+ ToolModule.addZeros(i + "", 3))));
					Graphics2D sg = sheet.createGraphics();

					w = sheet.getWidth() / 4;
					h = sheet.getHeight() / 4;
					spriteGroupDims[i] = new int[] { w, h };
					z = 0;
					for (y = 0; y < 2; ++y) {
						for (x = 0; x < 4; x += 2) {
							BufferedImage sp = new BufferedImage(w, h,
									BufferedImage.TYPE_INT_ARGB);
							Graphics2D g = sp.createGraphics();
							g.setComposite(sg.getComposite());
							g.drawImage(sheet, 0, 0, w, h, w * x, h * y, w * x
									+ w, h * y + h, null);
							g.dispose();
							spriteGroups[i][z] = sp;
							++z;
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public static class Door {
			public int x, y, eventFlag;
			public String type, pointer;
			// Rope/Ladder
			public int climbDir;
			// Door stuff
			public int destX, destY, destDir, style;

			public Door() {
				this.type = "ladder";
				this.pointer = "$0";
			}

			public Door(int x, int y, String type) {
				this.x = x;
				this.y = y;
				this.type = type;
			}

			public Door copy() {
				Door d = new Door();
				d.eventFlag = eventFlag;
				d.type = type;
				d.pointer = pointer;
				d.climbDir = climbDir;
				d.destX = destX;
				d.destY = destY;
				d.destDir = destDir;
				d.style = style;
				return d;
			}
		}

		private final String[] climbDirs = new String[] { "nw", "ne", "sw",
				"se", "nowhere" };
		private final String[] destDirs = new String[] { "down", "up", "right",
				"left" };

		private int indexOf(Object[] arr, Object target) {
			int i = 0;
			for (Object e : arr) {
				if (e.equals(target))
					return i;
				++i;
			}
			return -1;
		}

		private void importDoors(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<Integer, List<Map<String, Object>>>> doorsMap = (Map<Integer, Map<Integer, List<Map<String, Object>>>>) yaml
						.load(input);
				int y, x;
				ArrayList<Door> area;
				for (Map.Entry<Integer, Map<Integer, List<Map<String, Object>>>> rowEntry : doorsMap
						.entrySet()) {
					y = rowEntry.getKey();
					for (Map.Entry<Integer, List<Map<String, Object>>> entry : rowEntry
							.getValue().entrySet()) {
						x = entry.getKey();
						area = this.doorAreas[y][x];
						area.clear();
						if (entry.getValue() == null)
							continue;

						for (Map<String, Object> de : entry.getValue()) {
							Door d = new Door((Integer) de.get("X"),
									(Integer) de.get("Y"),
									((String) de.get("Type")).toLowerCase());
							if (d.type.equals("stairs")
									|| d.type.equals("escalator")) {
								d.climbDir = indexOf(climbDirs,
										de.get("Direction"));
							} else if (d.type.equals("door")) {
								d.pointer = (String) de.get("Text Pointer");
								d.eventFlag = (Integer) de.get("Event Flag");
								d.destX = (Integer) de.get("Destination X");
								d.destY = (Integer) de.get("Destination Y");
								d.destDir = indexOf(destDirs,
										((String) de.get("Direction"))
												.toLowerCase());
								d.style = (Integer) de.get("Style");
							} else if (d.type.equals("switch")) {
								d.pointer = (String) de.get("Text Pointer");
								d.eventFlag = (Integer) de.get("Event Flag");
							} else if (d.type.equals("person")
									|| d.type.equals("object")) {
								d.pointer = (String) de.get("Text Pointer");
							}
							area.add(d);
						}
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void exportDoors(File f) {
			Map<Integer, Map<Integer, List<Map<String, Object>>>> doorsMap = new HashMap<Integer, Map<Integer, List<Map<String, Object>>>>();
			int x, y = 0;
			for (List<Door>[] row : doorAreas) {
				Map<Integer, List<Map<String, Object>>> rowOut = new HashMap<Integer, List<Map<String, Object>>>();
				x = 0;
				for (List<Door> area : row) {
					if (area.isEmpty())
						rowOut.put(x, null);
					else {
						List<Map<String, Object>> areaOut = new ArrayList<Map<String, Object>>();
						for (Door d : area) {
							Map<String, Object> dOut = new HashMap<String, Object>();
							dOut.put("X", d.x);
							dOut.put("Y", d.y);
							dOut.put("Type", d.type);
							if (d.type.equals("stairway")
									|| d.type.equals("escalator")) {
								dOut.put("Direction", climbDirs[d.climbDir]);
							} else if (d.type.equals("door")) {
								dOut.put("Text Pointer", d.pointer);
								dOut.put("Event Flag", d.eventFlag);
								dOut.put("Destination X", d.destX);
								dOut.put("Destination Y", d.destY);
								dOut.put("Direction", destDirs[d.destDir]);
								dOut.put("Style", d.style);
							} else if (d.type.equals("switch")) {
								dOut.put("Text Pointer", d.pointer);
								dOut.put("Event Flag", d.eventFlag);
							} else if (d.type.equals("person")
									|| d.type.equals("object")) {
								dOut.put("Text Pointer", d.pointer);
							}
							areaOut.add(dOut);
						}
						rowOut.put(x, areaOut);
					}
					++x;
				}
				doorsMap.put(y, rowOut);
				++y;
			}

			try {
				FileWriter fw = new FileWriter(f);
				DumperOptions options = new DumperOptions();
				options.setDefaultFlowStyle(FlowStyle.BLOCK);
				Yaml yaml = new Yaml(options);
				yaml.dump(doorsMap, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void importSpritePlacements(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<Integer, List<Map<String, Integer>>>> spritesMap = (Map<Integer, Map<Integer, List<Map<String, Integer>>>>) yaml
						.load(input);
				int y, x;
				ArrayList<SpriteEntry> area;
				for (Map.Entry<Integer, Map<Integer, List<Map<String, Integer>>>> rowEntry : spritesMap
						.entrySet()) {
					y = rowEntry.getKey();
					for (Map.Entry<Integer, List<Map<String, Integer>>> entry : rowEntry
							.getValue().entrySet()) {
						x = entry.getKey();
						area = this.spriteAreas[y][x];
						area.clear();
						if (entry.getValue() == null)
							continue;

						for (Map<String, Integer> spe : entry.getValue()) {
							area.add(new SpriteEntry(spe.get("X"),
									spe.get("Y"), spe.get("NPC ID")));
						}
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void exportSpritePlacements(File f) {
			Map<Integer, Map<Integer, List<Map<String, Integer>>>> spritesMap = new HashMap<Integer, Map<Integer, List<Map<String, Integer>>>>();
			int x, y = 0;
			for (List<SpriteEntry>[] row : spriteAreas) {
				Map<Integer, List<Map<String, Integer>>> rowOut = new HashMap<Integer, List<Map<String, Integer>>>();
				x = 0;
				for (List<SpriteEntry> area : row) {
					if (area.isEmpty())
						rowOut.put(x, null);
					else {
						List<Map<String, Integer>> areaOut = new ArrayList<Map<String, Integer>>();
						for (SpriteEntry se : area) {
							Map<String, Integer> seOut = new HashMap<String, Integer>();
							seOut.put("X", se.x);
							seOut.put("Y", se.y);
							seOut.put("NPC ID", se.npcID);
							areaOut.add(seOut);
						}
						rowOut.put(x, areaOut);
					}
					++x;
				}
				spritesMap.put(y, rowOut);
				++y;
			}

			try {
				FileWriter fw = new FileWriter(f);
				DumperOptions options = new DumperOptions();
				options.setDefaultFlowStyle(FlowStyle.BLOCK);
				Yaml yaml = new Yaml(options);
				yaml.dump(spritesMap, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public static class Hotspot {
			int x1, y1, x2, y2;
		}

		private void importHotspots(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<String, Integer>> hsMap = (Map<Integer, Map<String, Integer>>) yaml
						.load(input);

				int i;
				for (Map.Entry<Integer, Map<String, Integer>> entry : hsMap
						.entrySet()) {
					i = entry.getKey();
					hotspots[i].x1 = entry.getValue().get("X1");
					hotspots[i].y1 = entry.getValue().get("Y1");
					hotspots[i].x2 = entry.getValue().get("X2");
					hotspots[i].y2 = entry.getValue().get("Y2");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void exportHotspots(File f) {
			Map<Integer, Map<String, Integer>> hsMap = new HashMap<Integer, Map<String, Integer>>();
			int i = 0;
			for (Hotspot hs : hotspots) {
				Map<String, Integer> entry = new HashMap<String, Integer>();
				entry.put("X1", hs.x1);
				entry.put("Y1", hs.y1);
				entry.put("X2", hs.x2);
				entry.put("Y2", hs.y2);
				hsMap.put(i, entry);
				++i;
			}

			try {
				FileWriter fw = new FileWriter(f);
				DumperOptions options = new DumperOptions();
				options.setDefaultFlowStyle(FlowStyle.BLOCK);
				Yaml yaml = new Yaml(options);
				yaml.dump(hsMap, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void importEnemyPlacement(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<String, Integer>> enemiesMap = (Map<Integer, Map<String, Integer>>) yaml
						.load(input);

				int y, x;
				for (Map.Entry<Integer, Map<String, Integer>> entry : enemiesMap
						.entrySet()) {
					y = entry.getKey() / (WIDTH_IN_TILES / 2);
					x = entry.getKey() % (WIDTH_IN_TILES / 2);
					enemyPlacement[y][x] = entry.getValue().get(
							"Enemy Map Group");
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void exportEnemyPlacement(File f) {
			Map<Integer, Map<String, Integer>> enemiesMap = new HashMap<Integer, Map<String, Integer>>();
			int i = 0;
			for (int[] row : enemyPlacement) {
				for (int ep : row) {
					Map<String, Integer> entry = new HashMap<String, Integer>();
					entry.put("Enemy Map Group", ep);
					enemiesMap.put(i, entry);
					++i;
				}
			}

			try {
				FileWriter fw = new FileWriter(f);
				DumperOptions options = new DumperOptions();
				options.setDefaultFlowStyle(FlowStyle.BLOCK);
				Yaml yaml = new Yaml(options);
				yaml.dump(enemiesMap, fw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void importSectors(File f) {
			InputStream input;
			try {
				input = new FileInputStream(f);
				Yaml yaml = new Yaml();
				Map<Integer, Map<String, Object>> sectorsMap = (Map<Integer, Map<String, Object>>) yaml
						.load(input);

				int y, x;
				Sector sec;
				for (Map.Entry<Integer, Map<String, Object>> entry : sectorsMap
						.entrySet()) {
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
			for (Sector[] row : sectors) {
				for (Sector s : row) {
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
						tmp = "" + ((char) in.read());
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
						tmp = ToolModule.addZeros(
								Integer.toHexString(mapTiles[i][j]), 3);
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
				tileset = 0;
				palette = 0;
				music = 0;
				item = 0;
				townmap = "none";
				setting = "none";
				teleport = "disabled";
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
				this.x = x;
				this.y = y;
				this.npcID = npcID;
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
			for (List<SpriteEntry>[] row : spriteAreas) {
				for (List<SpriteEntry> area : row) {
					area.clear();
				}
			}
		}

		public void nullMapData() {
			for (int i = 0; i < mapTiles.length; i++) {
				for (int j = 0; j < mapTiles[i].length; j++) {
					mapTiles[i][j] = 0;
				}
			}
			for (Sector[] row : sectors) {
				for (Sector s : row) {
					s.reset();
				}
			}
		}

		public void nullEnemyData() {
			for (int i = 0; i < enemyPlacement.length; ++i)
				for (int j = 0; j < enemyPlacement[i].length; ++j)
					enemyPlacement[i][j] = 0;
		}

		public void nullDoorData() {
			for (List<Door>[] row : doorAreas) {
				for (List<Door> area : row) {
					area.clear();
				}
			}
		}
	}

	public void load(Project proj) {
		map.load(proj);
	}

	public void save(Project proj) {
		map.save(proj);
	}

	public void refresh(Project proj) {
		map.loadExtraResources(proj);

		if (isInited)
			mapDisplay.repaint();
	}

	public void reset() {
		map.reset();
		if (mapDisplay != null)
			mapDisplay.reset();
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
		TileEditor.Tileset tileset = TileEditor.tilesets[TileEditor
				.getDrawTilesetNumber(mapTset)];
		TileEditor.Tileset.Palette pal;
		for (int i = 0; i < tileset.getPaletteCount(); i++) {
			if ((pal = tileset.getPalette(i)).getMapTileset() == mapTset) {
				palChooser.addItem(Integer.toString(pal.getMapPalette()));
			}
		}
		if (mapPal >= palChooser.getItemCount()) {
			palChooser.setSelectedIndex(palChooser.getItemCount() - 1);
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
			mapDisplay.setSelectedSectorTileset(tilesetChooser
					.getSelectedIndex());
			if (updatePaletteChooser(mapDisplay.getSelectedSector().tileset,
					mapDisplay.getSelectedSector().palette))
				mapDisplay.setSelectedSectorPalette(palChooser
						.getSelectedIndex());
			mapDisplay.repaint();
			tileSelector.repaint();
		} else if (e.getSource().equals(palChooser)) {
			mapDisplay.setSelectedSectorPalette(palChooser.getSelectedIndex());
			tileSelector.repaint();
		} else if (e.getSource().equals(musicChooser)) {
			mapDisplay.getSelectedSector().music = musicChooser
					.getSelectedIndex();
		} else if (e.getActionCommand().equals("apply")) {
			// TODO
		} else if (e.getActionCommand().equals("close")) {
			hide();
		} else if (e.getActionCommand().equals("mode0")) {
			mapDisplay.changeMode(0);
			mapDisplay.repaint();
			tileSelector.changeMode(0);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode1")) {
			mapDisplay.changeMode(1);
			mapDisplay.repaint();
			tileSelector.changeMode(1);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode2")) {
			mapDisplay.changeMode(2);
			mapDisplay.repaint();
			tileSelector.changeMode(2);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode6")) {
			mapDisplay.changeMode(6);
			mapDisplay.repaint();
			tileSelector.changeMode(6);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode7")) {
			mapDisplay.changeMode(7);
			mapDisplay.repaint();
			tileSelector.changeMode(7);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode8")) {
			mapDisplay.changeMode(8);
			mapDisplay.repaint();
			tileSelector.changeMode(8);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("mode9")) {
			mapDisplay.changeMode(9);
			mapDisplay.repaint();
			tileSelector.changeMode(9);
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("delAllSprites")) {
			int sure = JOptionPane
					.showConfirmDialog(
							mainWindow,
							"Are you sure you want to delete all of the sprites?\n"
									+ "Note that the game may crash if less than 8 sprites exist on the map.",
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
		} else if (e.getActionCommand().equals("delAllEverything")) {
			int sure = JOptionPane.showConfirmDialog(mainWindow,
					"Are you sure you want to all of the map data?",
					"Are you sure?", JOptionPane.YES_NO_OPTION);
			if (sure == JOptionPane.YES_OPTION) {
				map.nullMapData();
				map.nullSpriteData();
				map.nullDoorData();
				map.nullEnemyData();
				mapDisplay.repaint();
			}
		} else if (e.getActionCommand().equals("resetTileImages")) {
			MapDisplay.resetTileImageCache();
			mapDisplay.repaint();
			tileSelector.repaint();
		} else if (e.getActionCommand().equals("grid")) {
			mapDisplay.toggleGrid();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("spriteboxes")) {
			mapDisplay.toggleSpriteBoxes();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("tileNums")) {
			mapDisplay.toggleTileNums();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("enableTileDragging")) {
			mapDisplay.toggleTileDragging();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("toggleHighlighting")) {
			mapDisplay.toggleHighlighting();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("npcNums")) {
			mapDisplay.toggleSpriteNums();
			mapDisplay.repaint();
		} else if (e.getActionCommand().equals("mapchanges")) {
			mapDisplay.toggleMapChanges();
		} else if (e.getActionCommand().equals("sectorEdit")) {
			// net.starmen.pkhack.JHack.main.showModule(
			// MapSectorPropertiesEditor.class, gfxcontrol
			// .getSectorxy());
		} else if (e.getActionCommand().equals("findSprite")) {
			String tpt = JOptionPane.showInputDialog(mainWindow,
					"Enter TPT entry to search for.", Integer.toHexString(0));
			int tptNum, yesno;
			try {
				tptNum = Integer.parseInt(tpt, 16);
			} catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(mainWindow, "\"" + tpt
						+ "\" is not a valid hexidecimal number.\n"
						+ "Search was aborted.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			/*
			 * if (tpt != null) { for (int i = 0; i < (MapData.HEIGHT_IN_SECTORS
			 * / 2) MapData.WIDTH_IN_SECTORS; i++) { ArrayList sprites =
			 * map.getSpritesData(i); MapData.SpriteLocation spLoc; int areaY,
			 * areaX; for (int j = 0; j < sprites.size(); j++) { spLoc =
			 * (MapData.SpriteLocation) sprites.get(j); if (spLoc.getTpt() ==
			 * tptNum) { areaY = i / MapEditor.widthInSectors; areaX = i -
			 * (areaY * MapEditor.widthInSectors); gfxcontrol .setMapXY( (areaX
			 * * MapEditor.sectorWidth) + (spLoc.getX() / MapEditor.tileWidth),
			 * (areaY * MapEditor.sectorHeight * 2) + (spLoc.getY() /
			 * MapEditor.tileHeight)); yesno = JOptionPane .showConfirmDialog(
			 * mainWindow,
			 * "I found a sprite with that TPT entry. Do you want to find another?"
			 * , "Continue Search?", JOptionPane.YES_NO_OPTION); if (yesno ==
			 * JOptionPane.NO_OPTION) return; } } }
			 * JOptionPane.showMessageDialog(mainWindow,
			 * "Could not find a sprite entry using TPT entry 0x" + tpt + ".");
			 * }
			 */
		} else if (e.getActionCommand().equals("copySector")) {
			pasteSector.setEnabled(true);

			int sectorX = mapDisplay.getSectorX();
			int sectorY = mapDisplay.getSectorY();
			for (int i = 0; i < copiedSectorTiles.length; i++)
				for (int j = 0; j < copiedSectorTiles[i].length; j++)
					copiedSectorTiles[i][j] = map.getMapTile(j + sectorX * 8, i
							+ sectorY * 4);
			copiedSector = map.getSector(sectorX, sectorY);
		} else if (e.getActionCommand().equals("pasteSector")) {
			int sectorX = mapDisplay.getSectorX();
			int sectorY = mapDisplay.getSectorY();
			mapDisplay.pasteSector(copiedSector, sectorX, sectorY,
					copiedSectorTiles);
			mapDisplay.repaint();
			// gfxcontrol.updateComponents();
			/*
			 * } else if (ac.equals(ENEMY_SPRITES)) {
			 * gfxcontrol.toggleEnemySprites(); } else if
			 * (ac.equals(ENEMY_COLORS)) { gfxcontrol.toggleEnemyColors(); }
			 * else if (ac.equals(EVENTPAL)) { gfxcontrol.toggleEventPalette();
			 */
		} else if (e.getActionCommand().equals("undoMap")) {
			if (!mapDisplay.undoMapAction()) {
				JOptionPane.showMessageDialog(mainWindow,
						"There are no actions to undo.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getActionCommand().equals("redoMap")) {
			if (!mapDisplay.redoMapAction()) {
				JOptionPane.showMessageDialog(mainWindow,
						"There are no actions to redo.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if( e.getActionCommand().equals( "highlightMultipleTilesExternal" ) ) {
			mapDisplay.externalCommand( "highlightMultipleTiles" );
		} else if( e.getActionCommand().equals( "selectAreaExternal" ) ) {
			mapDisplay.externalCommand( "selectArea" );
		} else if( e.getActionCommand().equals( "selectSnakeyAreaExternal" ) ) {
			mapDisplay.externalCommand( "selectSnakeyArea" );
		}
	}

	public void changedUpdate(DocumentEvent e) {
		if ((e.getDocument().equals(xField.getDocument()) || e.getDocument()
				.equals(yField.getDocument()))
				&& (yField.getText().length() > 0)
				&& (xField.getText().length() > 0)) {
			int newX = Integer.parseInt(xField.getText()), newY = Integer
					.parseInt(yField.getText());
			if (newX > MapData.WIDTH_IN_TILES - mapDisplay.getScreenWidth()) {
				newX = MapData.WIDTH_IN_TILES - mapDisplay.getScreenWidth();
			} else if (newX < 0) {
				newX = 0;
			}
			if (newY > MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight()) {
				newY = MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight();
			} else if (newY < 0) {
				newY = 0;
			}
			if ((newX != mapDisplay.getMapX())
					|| (newY != mapDisplay.getMapY())) {
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
		if (e.isControlDown()) { // Horizontal scrolling
			int x = mapDisplay.getMapX() + (e.getWheelRotation() * 4);
			x = Math.min(x,
					MapData.WIDTH_IN_TILES - mapDisplay.getScreenWidth());
			x = Math.max(x, 0);
			if (x != mapDisplay.getMapX()) {
				mapDisplay.setMapX(x);
				updateXYScrollBars();
				updateXYFields();
				mapDisplay.repaint();
			}
		} else { // Vertical scrolling
			int y = mapDisplay.getMapY() + (e.getWheelRotation() * 4);
			y = Math.min(y,
					MapData.HEIGHT_IN_TILES - mapDisplay.getScreenHeight());
			y = Math.max(y, 0);
			if (y != mapDisplay.getMapY()) {
				mapDisplay.setMapY(y);
				updateXYScrollBars();
				updateXYFields();
				mapDisplay.repaint();
			}
		}

	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		Dimension newD = mapDisplay.getSize();
		int newSW = newD.width / 32, newSH = newD.height / 32;
		mapDisplay.setScreenSize(newSW, newSH);
		updateXYScrollBars();
		updateXYFields();
		tileSelector.setScreenSize(newSW);
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		// TODO Auto-generated method stub

	}
	
	private static List<Color> colorsFromInts = null;
	//returns colors for the highlighted tiles
	public static Color colorFromInt( int index ) {
		if( colorsFromInts == null ) {
			colorsFromInts = new ArrayList<Color>();
			colorsFromInts.add( Color.orange );
			colorsFromInts.add( Color.yellow );
			colorsFromInts.add( Color.green );
			colorsFromInts.add( new Color( 0, 255, 180 ) );
			colorsFromInts.add( Color.cyan );
			colorsFromInts.add( Color.blue );
			colorsFromInts.add( Color.magenta );
			colorsFromInts.add( Color.pink );
			colorsFromInts.add( Color.red );
		}
		return colorsFromInts.get( index % colorsFromInts.size() );
	}
}
