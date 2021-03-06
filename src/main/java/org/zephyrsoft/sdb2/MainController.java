/*
 * This file is part of the Song Database (SDB).
 * 
 * SDB is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * SDB is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SDB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.sdb2;

import static java.util.stream.Collectors.toList;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.core.animation.timing.Animator;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.swing.animation.timing.sources.SwingTimerTimingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyrsoft.sdb2.gui.MainWindow;
import org.zephyrsoft.sdb2.model.AddressablePart;
import org.zephyrsoft.sdb2.model.FilterTypeEnum;
import org.zephyrsoft.sdb2.model.ScreenContentsEnum;
import org.zephyrsoft.sdb2.model.SelectableScreen;
import org.zephyrsoft.sdb2.model.Song;
import org.zephyrsoft.sdb2.model.SongsModel;
import org.zephyrsoft.sdb2.model.XMLConverter;
import org.zephyrsoft.sdb2.model.settings.SettingKey;
import org.zephyrsoft.sdb2.model.settings.SettingsModel;
import org.zephyrsoft.sdb2.presenter.Presentable;
import org.zephyrsoft.sdb2.presenter.Presenter;
import org.zephyrsoft.sdb2.presenter.PresenterBundle;
import org.zephyrsoft.sdb2.presenter.PresenterWindow;
import org.zephyrsoft.sdb2.presenter.ScreenHelper;
import org.zephyrsoft.sdb2.presenter.Scroller;
import org.zephyrsoft.util.StringTools;
import org.zephyrsoft.util.gui.ErrorDialog;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

/**
 * Controller for {@link MainWindow}.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class MainController implements Scroller {
	
	private static Logger LOG = LoggerFactory.getLogger(MainController.class);
	
	private StatisticsController statisticsController;
	
	private String songsFileName = null;
	private SongsModel songs = null;
	private SettingsModel settings = null;
	
	private List<SelectableScreen> screens;
	private PresenterBundle presentationControl;
	private Song currentlyPresentedSong = null;
	
	private ExecutorService executor = Executors.newCachedThreadPool();
	private Future<?> countDownFuture;
	private Iterator<File> slideShowImages;
	private Future<?> slideShowFuture;
	private final static Pattern imagePattern = Pattern.compile("(?i)^.*\\.(png|jpg|jpeg|gif|bmp)$");
	
	public MainController(StatisticsController statisticsController) {
		this.statisticsController = statisticsController;
	}
	
	public boolean present(Presentable presentable) {
		// make it possible to end the old presentation (if any)
		PresenterBundle oldPresentationControl = null;
		if (presentationControl != null) {
			oldPresentationControl = presentationControl;
		}
		
		presentationControl = new PresenterBundle();
		
		Presenter presenter1 = createPresenter(
			ScreenHelper.getScreen(screens, settings.get(SettingKey.SCREEN_1_DISPLAY, Integer.class)), presentable,
			settings.get(SettingKey.SCREEN_1_CONTENTS, ScreenContentsEnum.class));
		if (presenter1 != null) {
			presentationControl.addPresenter(presenter1);
		}
		
		Presenter presenter2 = createPresenter(
			ScreenHelper.getScreen(screens, settings.get(SettingKey.SCREEN_2_DISPLAY, Integer.class)), presentable,
			settings.get(SettingKey.SCREEN_2_CONTENTS, ScreenContentsEnum.class));
		if (presenter2 != null) {
			presentationControl.addPresenter(presenter2);
		}
		
		if (presentationControl.isEmpty()) {
			ErrorDialog
				.openDialog(
					null,
					"Could not start presentation!\n\nPlease specify at least one existing presentation display:\nCheck your system configuration\nand/or adjust this program's configuration\n(see tab \"Global Settings\")!");
			return false;
		} else {
			currentlyPresentedSong = presentable.getSong();
			
			if (currentlyPresentedSong != null) {
				startCountDown(settings.get(SettingKey.SECONDS_UNTIL_COUNTED, Integer.class), currentlyPresentedSong);
			} else {
				stopCountDown();
			}
			
			// start presentation
			presentationControl.showPresenter();
			
			// now stop old presentation (if any)
			if (oldPresentationControl != null) {
				oldPresentationControl.hidePresenter();
			}
			
			return true;
		}
	}
	
	public void startCountDown(final int seconds, final Song song) {
		Runnable countDownRunnable = () -> {
			LOG.debug("start sleeping for {} seconds (count-down)", seconds);
			try {
				Thread.sleep(seconds * 1000);
				statisticsController.countSongAsPresentedToday(song);
			} catch (InterruptedException e) {
				// if interrupted, do nothing (the countdown was stopped)
				LOG.debug("interrupted (count-down)");
			}
		};
		stopCountDown();
		if (executor.isShutdown()) {
			throw new IllegalStateException("background executor is stopped");
		} else {
			countDownFuture = executor.submit(countDownRunnable);
		}
	}
	
	public void stopCountDown() {
		if (countDownFuture != null) {
			LOG.debug("stopping countdown");
			countDownFuture.cancel(true);
			countDownFuture = null;
		} else {
			LOG.trace("wanted to stop countdown, but nothing to do");
		}
	}
	
	@Override
	public List<AddressablePart> getParts() {
		Preconditions.checkArgument(presentationControl != null, "there is no active presentation");
		return presentationControl.getParts();
	}
	
	@Override
	public void moveToPart(Integer part) {
		presentationControl.moveToPart(part);
	}
	
	@Override
	public void moveToLine(Integer part, Integer line) {
		presentationControl.moveToLine(part, line);
	}
	
	private PresenterWindow createPresenter(SelectableScreen screen, Presentable presentable,
		ScreenContentsEnum contents) {
		if (screen == null || !screen.isAvailable()) {
			// nothing to be done
			return null;
		}
		return new PresenterWindow(screen, presentable, contents, settings);
	}
	
	public List<SelectableScreen> getScreens() {
		return Collections.unmodifiableList(screens);
	}
	
	public void detectScreens() {
		if (screens == null) {
			screens = new ArrayList<>();
		} else {
			screens.clear();
		}
		
		// for the setting "don't show at all"
		screens.add(null);
		
		List<SelectableScreen> availableScreens = ScreenHelper.getScreens();
		screens.addAll(availableScreens);
		
		// if applicable: add currently unavailable screens if they are mentioned in settings
		Integer screen1Index = settings.get(SettingKey.SCREEN_1_DISPLAY, Integer.class);
		Integer screen2Index = settings.get(SettingKey.SCREEN_2_DISPLAY, Integer.class);
		int maxScreenIndex = Math.max(screen1Index == null ? 0 : screen1Index, screen2Index == null ? 0 : screen2Index);
		for (int i = availableScreens.size(); i <= maxScreenIndex; i++) {
			LOG.debug("adding screen with index {} as unavailable", i);
			screens.add(new SelectableScreen(i, false));
		}
	}
	
	public boolean prepareClose() {
		LOG.debug("preparing to close application");
		return saveAll();
	}
	
	public boolean saveAll() {
		boolean successfullySavedSongs = saveSongs();
		boolean successfullySavedSettings = saveSettings();
		boolean successfullySavedStatistics = statisticsController.saveStatistics();
		return successfullySavedSongs && successfullySavedSettings && successfullySavedStatistics;
	}
	
	public void shutdown() {
		shutdown(0);
	}
	
	public void shutdown(int exitCode) {
		LOG.debug("closing application, exit code {}", exitCode);
		executor.shutdownNow();
		System.exit(exitCode);
	}
	
	public void loadSongs(String fileName) {
		if (!StringTools.isBlank(fileName)) {
			songsFileName = fileName;
		}
		songs = populateSongsModel();
		if (songs == null) {
			// there was a problem while reading
			songs = new SongsModel();
		}
	}
	
	public void exportStatisticsAll(File targetExcelFile) {
		statisticsController.exportStatisticsAll(songs, targetExcelFile);
	}
	
	public void loadSettings() {
		LOG.debug("loading settings from file");
		File file = new File(FileAndDirectoryLocations.getSettingsFileName());
		try {
			InputStream xmlInputStream = new FileInputStream(file);
			settings = XMLConverter.fromXMLToSettingsModel(xmlInputStream);
			xmlInputStream.close();
		} catch (IOException e) {
			LOG.error("could not read settings from \"" + file.getAbsolutePath() + "\"");
		}
		if (settings == null) {
			// there was a problem while reading
			settings = new SettingsModel();
		}
		loadDefaultSettingsForUnsetSettings();
	}
	
	private void loadDefaultSettingsForUnsetSettings() {
		putDefaultIfKeyIsUnset(SettingKey.BACKGROUND_COLOR, Color.BLACK);
		putDefaultIfKeyIsUnset(SettingKey.TEXT_COLOR, Color.WHITE);
		
		putDefaultIfKeyIsUnset(SettingKey.TOP_MARGIN, Integer.valueOf(10));
		putDefaultIfKeyIsUnset(SettingKey.LEFT_MARGIN, Integer.valueOf(0));
		putDefaultIfKeyIsUnset(SettingKey.RIGHT_MARGIN, Integer.valueOf(0));
		putDefaultIfKeyIsUnset(SettingKey.BOTTOM_MARGIN, Integer.valueOf(20));
		putDefaultIfKeyIsUnset(SettingKey.DISTANCE_TITLE_TEXT, Integer.valueOf(20));
		putDefaultIfKeyIsUnset(SettingKey.DISTANCE_TEXT_COPYRIGHT, Integer.valueOf(20));
		
		putDefaultIfKeyIsUnset(SettingKey.SONG_LIST_FILTER, FilterTypeEnum.TITLE_AND_LYRICS);
		putDefaultIfKeyIsUnset(SettingKey.SCREEN_1_CONTENTS, ScreenContentsEnum.ONLY_LYRICS);
		putDefaultIfKeyIsUnset(SettingKey.SCREEN_2_CONTENTS, ScreenContentsEnum.LYRICS_AND_CHORDS);
		List<SelectableScreen> availableScreens = ScreenHelper.getScreens();
		if (availableScreens.size() > 1) {
			putDefaultIfKeyIsUnset(SettingKey.SCREEN_1_DISPLAY, Integer.valueOf(availableScreens.get(1).getIndex()));
		}
		
		putDefaultIfKeyIsUnset(SettingKey.SHOW_TITLE, Boolean.TRUE);
		putDefaultIfKeyIsUnset(SettingKey.TITLE_FONT, new Font(Font.SERIF, Font.BOLD, 10));
		putDefaultIfKeyIsUnset(SettingKey.LYRICS_FONT, new Font(Font.SERIF, Font.PLAIN, 10));
		putDefaultIfKeyIsUnset(SettingKey.TRANSLATION_FONT, new Font(Font.SERIF, Font.PLAIN, 10));
		putDefaultIfKeyIsUnset(SettingKey.COPYRIGHT_FONT, new Font(Font.SERIF, Font.ITALIC, 10));
		putDefaultIfKeyIsUnset(SettingKey.LOGO_FILE, "");
		putDefaultIfKeyIsUnset(SettingKey.SECONDS_UNTIL_COUNTED, Integer.valueOf(60));
		
		putDefaultIfKeyIsUnset(SettingKey.SLIDE_SHOW_DIRECTORY, null);
		putDefaultIfKeyIsUnset(SettingKey.SLIDE_SHOW_SECONDS_UNTIL_NEXT_PICTURE, Integer.valueOf(20));
		
		// check that really all settings are set
		for (SettingKey key : SettingKey.values()) {
			if (!settings.isSet(key)) {
				throw new IllegalStateException("unset value for setting key: " + key);
			}
		}
	}
	
	private void putDefaultIfKeyIsUnset(SettingKey key, Object defaultValue) {
		if (!settings.isSet(key)) {
			settings.put(key, defaultValue);
		}
	}
	
	public synchronized boolean saveSettings() {
		File file = new File(FileAndDirectoryLocations.getSettingsFileName());
		try {
			OutputStream xmlOutputStream = new FileOutputStream(file);
			XMLConverter.fromSettingsModelToXML(settings, xmlOutputStream);
			xmlOutputStream.close();
			return true;
		} catch (IOException e) {
			LOG.error("could not write settings to \"" + file.getAbsolutePath() + "\"");
			return false;
		}
	}
	
	private SongsModel populateSongsModel() {
		File file = new File(getSongsFileName());
		LOG.debug("loading songs from file {}", file.getAbsolutePath());
		
		try {
			InputStream xmlInputStream = new FileInputStream(file);
			SongsModel modelToReturn = XMLConverter.fromXMLToSongsModel(xmlInputStream);
			xmlInputStream.close();
			return modelToReturn;
		} catch (Exception e) {
			LOG.error("could not read songs from " + file.getAbsolutePath(), e);
			ErrorDialog.openDialogBlocking(null, "Could not load songs from file\n" + file.getAbsolutePath());
			shutdown(-1);
			return null;
		}
	}
	
	public synchronized boolean saveSongs() {
		File file = new File(getSongsFileName());
		try {
			OutputStream xmlOutputStream = new FileOutputStream(file);
			XMLConverter.fromSongsModelToXML(songs, xmlOutputStream);
			xmlOutputStream.close();
			return true;
		} catch (IOException e) {
			LOG.error("could not write songs to \"" + file.getAbsolutePath() + "\"");
			return false;
		}
	}
	
	public SongsModel getSongs() {
		return songs;
	}
	
	public SettingsModel getSettings() {
		return settings;
	}
	
	private String getSongsFileName() {
		if (songsFileName == null) {
			return FileAndDirectoryLocations.getDefaultSongsFileName();
		} else {
			return songsFileName;
		}
	}
	
	/**
	 * Use a nice LaF.
	 * 
	 * @return {@code true} if the LaF could be applied, {@code false} otherwise
	 */
	public boolean setupLookAndFeel() {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
			return true;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
			| UnsupportedLookAndFeelException e) {
			LOG.warn("could not apply the look-and-feel");
			return false;
		}
	}
	
	public Song getCurrentlyPresentedSong() {
		return currentlyPresentedSong;
	}
	
	public static void initAnimationTimer() {
		final TimingSource animationTimer = new SwingTimerTimingSource(5, TimeUnit.MILLISECONDS);
		Animator.setDefaultTimingSource(animationTimer);
		animationTimer.init();
	}
	
	public void startSlideShowCycle(final int seconds) {
		Runnable slideShowRunnable = () -> {
			LOG.debug("start sleeping for {} seconds (slide show)", seconds);
			try {
				if (slideShowImages == null || !slideShowImages.hasNext()) {
					LOG.info("no image files available for slide show");
					return;
				}
				showSlide(slideShowImages.next());
				Thread.sleep(seconds * 1000);
				startSlideShowCycle(seconds);
			} catch (InterruptedException e) {
				// if interrupted, do nothing (the slide show was stopped)
				LOG.debug("interrupted (slide show)");
			}
		};
		if (executor.isShutdown()) {
			throw new IllegalStateException("background executor is stopped");
		} else {
			slideShowFuture = executor.submit(slideShowRunnable);
		}
	}
	
	public Image loadLogo() throws IOException {
		String logoPath = settings.get(SettingKey.LOGO_FILE, String.class);
		if (logoPath != null && !logoPath.equals("")) {
			File logoFile = new File(logoPath);
			return readImage(logoFile);
		}
		return null;
	}
	
	public Image readImage(File imageFile) throws IOException {
		if (imageFile.isFile() && imageFile.canRead()) {
			return ImageIO.read(imageFile);
		} else {
			return null;
		}
	}
	
	private void showSlide(File imageFile) {
		Image image = null;
		try {
			image = readImage(imageFile);
		} catch (IOException ioe) {
			LOG.warn("couldn't load image file {}", imageFile);
		}
		present(new Presentable(null, image));
	}
	
	public void stopSlideShow() {
		if (slideShowFuture != null) {
			LOG.debug("stopping slide show");
			slideShowFuture.cancel(true);
			slideShowFuture = null;
			slideShowImages = null;
		} else {
			LOG.trace("wanted to stop slide show, but nothing to do");
		}
	}
	
	public boolean presentSlideShow() {
		Path imageDirectory = Paths.get(settings.get(SettingKey.SLIDE_SHOW_DIRECTORY, String.class));
		if (imageDirectory == null || !Files.exists(imageDirectory) || !Files.isReadable(imageDirectory) || !Files.isDirectory(imageDirectory)) {
			LOG.warn("directory {} could not be opened", imageDirectory);
			return false;
		}
		
		slideShowImages = getImageIterator(imageDirectory);
		if (slideShowImages == null) {
			LOG.warn("images could not be loaded from directory {}", imageDirectory.toString());
			return false;
		}
		
		Integer seconds = settings.get(SettingKey.SLIDE_SHOW_SECONDS_UNTIL_NEXT_PICTURE, Integer.class);
		if (seconds == null) {
			LOG.warn("slide show settings: seconds until next picture missing");
			return false;
		}
		
		// stop the "old" slide show if it is still running
		stopSlideShow();
		
		startSlideShowCycle(seconds);
		return true;
	}
	
	private Iterator<File> getImageIterator(Path imageDirectory) {
		try {
			List<File> images = Files.list(imageDirectory)
				.map(path -> path.toFile())
				.filter(file -> imagePattern.matcher(file.getName()).matches())
				.sorted()
				.collect(toList());
			return Iterables.cycle(images).iterator();
		} catch (IOException e) {
			return null;
		}
	}
	
}
