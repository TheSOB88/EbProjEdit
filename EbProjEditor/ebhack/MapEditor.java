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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MapEditor extends ToolModule implements ActionListener, DocumentListener, AdjustmentListener, MouseWheelListener {
	
	private JTextField xField, yField;
	private JComboBox tilesetChooser, palChooser, musicChooser;
	private JScrollBar xScroll, yScroll;
	
	public static Map map;
	private MapDisplay mapDisplay;
	private TileSelector tileSelector;
	
	public MapEditor(YMLPreferences prefs) {
		super(prefs);
		
		map = new Map();
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

		JPanel contentPanel = new JPanel(new BorderLayout());
        
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(new JLabel("X: "));
		xField = ToolModule.createSizedJTextField(Integer.toString(Map.WIDTH_IN_TILES).length(), true);
		xField.getDocument().addDocumentListener(this);
		panel.add(xField);
		panel.add(new JLabel("Y: "));
		yField = ToolModule.createSizedJTextField(Integer.toString(Map.HEIGHT_IN_TILES).length(), true);
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
		
		mapDisplay = new MapDisplay(map);
		mapDisplay.addMouseWheelListener(this);
		mapDisplay.addActionListener(this);
		mapDisplay.init();
		contentPanel.add(mapDisplay, BorderLayout.CENTER);
		
		xScroll = new JScrollBar(JScrollBar.HORIZONTAL, 0, mapDisplay.getScreenWidth(), 0, Map.WIDTH_IN_TILES);
		xScroll.addAdjustmentListener(this);
		contentPanel.add(xScroll, BorderLayout.SOUTH);
		yScroll = new JScrollBar(JScrollBar.VERTICAL, 0, mapDisplay.getScreenHeight(), 0, Map.HEIGHT_IN_TILES);
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
		for (int i = 0; i < Map.NUM_MAP_TSETS; i++)
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
		private Map map;
		
		private final ActionEvent sectorEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "sectorChanged");
		
		private static Image[][][] tileImageCache;
		
		private int screenWidth = 24;
		private int screenHeight = 12;
		
		// Map X and Y coordinates of the tile displayed in the top left corner
		private int x = 0, y = 0;
		// Data for the selected sector
		private Map.Sector selectedSector = null;
		private int sectorX, sectorY;
		private int sectorPal;
		
		private TileSelector tileSelector;
		
		public MapDisplay(Map map) {
			super();
			this.map = map;
			
			if (tileImageCache == null)
				resetTileImageCache();
			
			addMouseListener(this);
			
			setPreferredSize(new Dimension(
					screenWidth * Map.TILE_WIDTH + 3,
					screenHeight * Map.TILE_HEIGHT + 3));
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
					screenWidth * Map.TILE_WIDTH + 2,
					screenHeight * Map.TILE_HEIGHT + 2));
		}
		
		private void drawMap(Graphics2D g) {
			g.setPaint(Color.white);
			g.setFont(new Font("Arial", Font.PLAIN, 12));
			
			Map.Sector sector;
			int pal;
			for (int i = 0; i < screenHeight; i++) {
				for (int j = 0; j < screenWidth; j++) {

					sector = map.getSector(
							(j + x)/Map.SECTOR_WIDTH,
							(i + y)/Map.SECTOR_HEIGHT);
					pal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(sector.tileset)].getPaletteNum(
							sector.tileset, sector.palette);
					g.drawImage(
							getTileImage(TileEditor.getDrawTilesetNumber(sector.tileset),
									map.getMapTile(x+j, y+i), pal),
							j*Map.TILE_WIDTH + 1, i*Map.TILE_HEIGHT + 1,
							Map.TILE_WIDTH, Map.TILE_HEIGHT,
							this);
				}
			}
			
			drawGrid(g);
			
			if (selectedSector != null) {
				int sXt, sYt;
				if (((sXt = sectorX * Map.SECTOR_WIDTH) + Map.SECTOR_WIDTH >= x)
						&& (sXt < x+screenWidth)
						&& ((sYt = sectorY * Map.SECTOR_HEIGHT) + Map.SECTOR_HEIGHT >= y)
						&& (sYt < y+screenHeight)) {
					g.setPaint(Color.yellow);
					g.draw(new Rectangle2D.Double(
							(sXt - x) * Map.TILE_WIDTH + 1,
							(sYt - y) * Map.TILE_HEIGHT + 1,
							Map.SECTOR_WIDTH * Map.TILE_WIDTH,
							Map.SECTOR_HEIGHT * Map.TILE_HEIGHT));
				}
			}
		}
		
		private void drawGrid(Graphics2D g) {
			g.setPaint(Color.black);
			// Draw vertical lines
			for (int i = 0; i < screenWidth+1; i++)
				g.drawLine(1 + i * Map.TILE_WIDTH, 1, 1 + i * Map.TILE_WIDTH, screenHeight * Map.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < screenHeight+1; i++)
				g.drawLine(1, 1 + i * Map.TILE_HEIGHT, screenWidth * Map.TILE_WIDTH, 1 + i * Map.TILE_HEIGHT);
			
			// Blank pixel in the bottom right corner
			g.drawLine(screenWidth * Map.TILE_WIDTH + 1,
					screenHeight * Map.TILE_HEIGHT + 1,
					screenWidth * Map.TILE_WIDTH + 1,
					screenHeight * Map.TILE_HEIGHT + 1);
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
		
		public Map.Sector getSelectedSector() {
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
		
		private void selectSector(int sX, int sY) {
			sectorX = sX;
			sectorY = sY;
			Map.Sector newS = map.getSector(sectorX, sectorY);
			if (selectedSector != newS) {
				selectedSector = newS;
				sectorPal = TileEditor.tilesets[TileEditor.getDrawTilesetNumber(selectedSector.tileset)].getPaletteNum(
						selectedSector.tileset, selectedSector.palette);
			} else {
				// Un-select sector
				selectedSector = null;
			}
			repaint();
			this.fireActionPerformed(sectorEvent);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// Make sure they didn't click on the border
			if ((e.getX() >= 1) && (e.getX() <= screenWidth * Map.TILE_WIDTH + 2)
					&& (e.getY() >= 1) && (e.getY() <= screenHeight * Map.TILE_HEIGHT + 2)) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					int mX = (e.getX() - 1) / Map.TILE_WIDTH + x;
					int mY = (e.getY() - 1) / Map.TILE_HEIGHT + y;
					if (e.isShiftDown()) {
						tileSelector.selectTile(map.getMapTile(mX, mY));
					} else {
						map.setMapTile(mX, mY, tileSelector.getSelectedTile());
						repaint();
					}
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					// Make sure they didn't click on the border
					int sX = (x + ((e.getX() - 1) / Map.TILE_WIDTH)) / Map.SECTOR_WIDTH;
					int sY = (y + ((e.getY() - 1) / Map.TILE_HEIGHT)) / Map.SECTOR_HEIGHT;
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
					width * Map.TILE_WIDTH + 3,
					height * Map.TILE_HEIGHT + 3));
			
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
								i*Map.TILE_WIDTH + 1, j*Map.TILE_HEIGHT + 1,
								Map.TILE_WIDTH, Map.TILE_HEIGHT,
								this);
						if (dtile == tile) {
							g.setPaint(Color.yellow);
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F));
							g.fillRect(i*Map.TILE_WIDTH + 1, j*Map.TILE_HEIGHT + 1,
									Map.TILE_WIDTH, Map.TILE_HEIGHT);
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
				g.drawLine(1 + i * Map.TILE_WIDTH, 1, 1 + i * Map.TILE_WIDTH, height * Map.TILE_HEIGHT);
			// Draw horizontal lines
			for (int i = 0; i < height+1; i++)
				g.drawLine(1, 1 + i * Map.TILE_HEIGHT, width * Map.TILE_WIDTH, 1 + i * Map.TILE_HEIGHT);
			
			// Blank pixel in the bottom right corner
			g.drawLine(width * Map.TILE_WIDTH + 1,
					height * Map.TILE_HEIGHT + 1,
					width * Map.TILE_WIDTH + 1,
					height * Map.TILE_HEIGHT + 1);
			
			// Draw border
			g.setColor(Color.black);
			g.draw(new Rectangle2D.Double(0, 0,
					width * Map.TILE_WIDTH + 2,
					height * Map.TILE_HEIGHT + 2));
		}
		
		public void adjustmentValueChanged(AdjustmentEvent e) {
			repaint();
		}

		public void mouseClicked(MouseEvent e) {
			if ((e.getButton() == MouseEvent.BUTTON1) && isEnabled()) {
				tile = (((e.getX()-1) / Map.TILE_WIDTH) + scroll.getValue()) * height
							+ ((e.getY()-1) / Map.TILE_HEIGHT);
				repaint();
			}
		}

		public void mousePressed(MouseEvent e) { }

		public void mouseReleased(MouseEvent e) { }

		public void mouseEntered(MouseEvent e) { }

		public void mouseExited(MouseEvent e) { }
    }
	
	public static class Map {
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
		
		public Map() {
			reset();
		}
		
		public void reset() {
			mapTiles = new int[HEIGHT_IN_TILES][WIDTH_IN_TILES];
			sectors = new Sector[HEIGHT_IN_SECTORS][WIDTH_IN_SECTORS];
			for (int i = 0; i < sectors.length; i++)
				for (int j = 0; j < sectors[i].length; j++)
					sectors[i][j] = new Sector();
		}
		
		public void load(Project proj) {
			importMapTiles(new File(proj.getFilename("eb.MapModule", "map_tiles")));
			importSectorTilesets(new File(proj.getFilename("eb.MapModule", "map_sector_tsets"))); // TODO
			importSectorMusic(new File(proj.getFilename("eb.MiscTablesModule", "map_sector_music")));
		}
		
		public void save(Project proj) {
			exportMapTiles(new File(proj.getFilename("eb.MapModule", "map_tiles")));
			exportSectorTilesets(new File(proj.getFilename("eb.MapModule", "map_sector_tsets")));
			exportSectorMusic(new File(proj.getFilename("eb.MiscTablesModule", "map_sector_music")));
		}
		
		public Sector getSector(int sectorX, int sectorY) {
			return sectors[sectorY][sectorX];
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
		
		private void importSectorTilesets(File f) {
	        /*if (f == null)
	            return;
            try {
    			Document dom = new SAXBuilder().build(f);
    			List nl = dom.getRootElement().getChildren("Sector");
    			ListIterator<Element> li = nl.listIterator();
    			Element e;
    			int num;
    			while (li.hasNext()) {
    				e = li.next();
    				num = ToolModule.parseUserInt(e.getAttributeValue("id"));
    				sectors[num/WIDTH_IN_SECTORS][num%WIDTH_IN_SECTORS].tileset = ToolModule.parseUserInt(e.getChildText("Tileset"));
    				sectors[num/WIDTH_IN_SECTORS][num%WIDTH_IN_SECTORS].palette = ToolModule.parseUserInt(e.getChildText("Palette"));
    			}
    		} catch (JDOMException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}*/
		}
		
		private void exportSectorTilesets(File f) {
	        /*if (f == null)
	            return;
        	Element root = new Element("Table");
        	root.setAttribute("id", "sector_tsets");
        	Document dom = new Document(root);
        	int id = 0;
        	Element entry, field;
        	for (int i = 0; i < sectors.length; i++) {
        		for (int j = 0; j < sectors[i].length; j++) {
        			entry = new Element("Sector");
        			entry.setAttribute("id", Integer.toString(id++));
        			field = new Element("Tileset");
        			field.setText(Integer.toString(sectors[i][j].tileset));
        			entry.addContent(field);
        			field = new Element("Palette");
        			field.setText(Integer.toString(sectors[i][j].palette));
        			entry.addContent(field);
        			root.addContent(entry);
        		}
        	}
            try {
            	FileOutputStream fos = new FileOutputStream(f);
                new XMLOutputter(Format.getPrettyFormat()).output(dom.getDocument(), fos);
            } catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}*/
		}
		
		private void exportSectorMusic(File f) {
	        /*if (f == null)
	            return;
        	Element root = new Element("Table");
        	root.setAttribute("id", "sector_music");
        	Document dom = new Document(root);
        	int id = 0;
        	Element entry, field;
        	for (int i = 0; i < sectors.length; i++) {
        		for (int j = 0; j < sectors[i].length; j++) {
        			entry = new Element("Sector");
        			entry.setAttribute("id", Integer.toString(id++));
        			field = new Element("Music");
        			field.setText(Integer.toString(sectors[i][j].music));
        			entry.addContent(field);
        			root.addContent(entry);
        		}
        	}
            try {
            	FileOutputStream fos = new FileOutputStream(f);
                new XMLOutputter(Format.getPrettyFormat()).output(dom.getDocument(), fos);
            } catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}*/
		}
		
		private void importSectorMusic(File f) {
	        /*if (f == null)
	            return;
            try {
    			Document dom = new SAXBuilder().build(f);
    			List nl = dom.getRootElement().getChildren("Sector");
    			ListIterator<Element> li = nl.listIterator();
    			Element e;
    			int num;
    			while (li.hasNext()) {
    				e = li.next();
    				num = ToolModule.parseUserInt(e.getAttributeValue("id"));
    				sectors[num/WIDTH_IN_SECTORS][num%WIDTH_IN_SECTORS].music = ToolModule.parseUserInt(e.getChildText("Music"));
    			}
    		} catch (JDOMException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}*/
		}
		
		private void setMapTilesFromStream(InputStream in) {
			int tmp1, tmp2;
			try {
				for (int i = 0; i < mapTiles.length; i++) {
					for (int j = 0; j < mapTiles[i].length; j++) {
						tmp1 = in.read();
						tmp2 = in.read();
						mapTiles[i][j] = tmp1 + (tmp2<<8);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void writeMapTilesToStream(FileOutputStream out) {
			try {
				for (int i = 0; i < mapTiles.length; i++) {
					for (int j = 0; j < mapTiles[i].length; j++) {
						out.write(mapTiles[i][j] & 0xff);
						out.write((mapTiles[i][j]>>8) & 0x3);
					}
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
			public int tileset = 0, palette = 0, music = 0;
			//public int townmap, misc, item;
			//public boolean cantTeleport, unknown;
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
			Map.Sector sect = mapDisplay.getSelectedSector();
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
        }
	}

	public void changedUpdate(DocumentEvent e) {
		if ((e.getDocument().equals(xField.getDocument())
					|| e.getDocument().equals(yField.getDocument()))
				&& (yField.getText().length() > 0)
				&& (xField.getText().length() > 0)) {
			int newX = Integer.parseInt(xField.getText()),
				newY = Integer.parseInt(yField.getText());
			if (newX > Map.WIDTH_IN_TILES - mapDisplay.getScreenWidth()) {
				newY = Map.WIDTH_IN_TILES - mapDisplay.getScreenHeight();
			} else if (newY < 0) {
				newY = 0;
			}
			if (newY > Map.HEIGHT_IN_TILES - mapDisplay.getScreenHeight()) {
				newY = Map.HEIGHT_IN_TILES - mapDisplay.getScreenHeight();
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
		if (y > Map.HEIGHT_IN_TILES - mapDisplay.getScreenHeight())
			y = Map.HEIGHT_IN_TILES - mapDisplay.getScreenHeight();
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
