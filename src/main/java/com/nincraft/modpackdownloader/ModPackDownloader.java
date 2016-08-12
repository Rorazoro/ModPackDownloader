package com.nincraft.modpackdownloader;

import java.io.File;

import org.apache.commons.collections4.CollectionUtils;

import com.beust.jcommander.JCommander;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nincraft.modpackdownloader.handler.ApplicationUpdateHandler;
import com.nincraft.modpackdownloader.manager.ModPackManager;
import com.nincraft.modpackdownloader.processor.DownloadModsProcessor;
import com.nincraft.modpackdownloader.processor.MergeManifestsProcessor;
import com.nincraft.modpackdownloader.processor.UpdateModsProcessor;
import com.nincraft.modpackdownloader.util.Arguments;
import com.nincraft.modpackdownloader.util.FileSystemHelper;
import com.nincraft.modpackdownloader.util.Reference;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class ModPackDownloader {
	public static void main(final String[] args) throws InterruptedException {

		initArguments(args);

		if (Arguments.clearCache) {
			FileSystemHelper.clearCache();
			return;
		}
		if (Arguments.updateApp) {
			ApplicationUpdateHandler.update();
			return;
		}

		setupRepo();

		if (Arguments.updateCurseModPack) {
			if (ModPackManager.updateModPack()) {
				ModPackManager.checkPastForgeVersion();
				ModPackManager.handlePostDownload();
			}
			return;
		}

		processManifests();
	}

	private static void processManifests() throws InterruptedException {
		log.trace("Processing Manifests...");

		updateMods();
		downloadMods();
		mergeManifests();

		log.trace("Finished Processing Manifests.");
	}

	private static void updateMods() throws InterruptedException {
		if (Arguments.shouldUpdateManifests()) {
			new UpdateModsProcessor(Arguments.manifestsToUpdate).process();
		}
	}

	private static void downloadMods() throws InterruptedException {
		if (Arguments.shouldDownloadManifests()) {
			new DownloadModsProcessor(Arguments.manifestsToDownload).process();
		}
	}

	private static void mergeManifests() throws InterruptedException {
		if (Arguments.shouldMergeManifests()) {
			new MergeManifestsProcessor(Arguments.manifestsToMerge).process();
		}
	}

	private static void initArguments(final String[] args) {
		// Initialize application arguments
		new JCommander(new Arguments(), args);

		// Set default application arguments
		defaultArguments();
	}

	private static void defaultArguments() {
		if (CollectionUtils.isEmpty(Arguments.manifestsToDownload)) {
			log.info(String.format("No manifest supplied, using default %s", Reference.DEFAULT_MANIFEST_FILE));

			Arguments.manifestsToDownload = Lists.newArrayList(new File(Reference.DEFAULT_MANIFEST_FILE));
		}
		if (Strings.isNullOrEmpty(Arguments.modFolder)) {
			log.info("No output folder supplied, using default \"mods\"");
			Arguments.modFolder = "mods";
		}
	}

	private static void setupRepo() {
		log.trace("Setting up local repository...");
		Reference.userhome = System.getProperty("user.home");
		log.debug(String.format("User Home System Property detected as: %s", Reference.userhome));

		Reference.os = System.getProperty("os.name");
		log.debug(String.format("Operating System detected as: %s", Reference.os));

		if (Reference.os.startsWith("Windows")) {
			Reference.userhome += Reference.WINDOWS_FOLDER;
		} else if (Reference.os.startsWith("Mac")) {
			Reference.userhome += Reference.MAC_FOLDER;
		} else {
			Reference.userhome += Reference.OTHER_FOLDER;
		}
		log.debug(String.format("User Home Folder set to: %s", Reference.userhome));

		FileSystemHelper.createFolder(Reference.userhome);

		log.debug("Setting User Agent...");
		System.setProperty("http.agent", "Mozilla/4.0");

		log.trace("Finished setting up local repository.");
	}

	/*private static void processMods() throws InterruptedException {
		log.trace("Processing Mods...");
		int returnCode = ModListManager.buildModList();
		if (returnCode == -1) {
			return;
		}
		if (CollectionUtils.isNotEmpty(Arguments.manifestsToUpdate)) {
			if (Strings.isNullOrEmpty(Arguments.mcVersion)) {
				log.error("No Minecraft version found in manifest file");
				return;
			}

			log.info(String.format("Updating mods with parameters: %s, %s, %s", Arguments.manifestsToDownload,
					Arguments.mcVersion, Arguments.releaseType));
			ModListManager.updateMods();

			waitFinishProcessingMods();

			ModListManager.updateManifest();
			log.info("Finished updating mods.");
		} else {
			log.info(String.format("Downloading mods with parameters: %s, %s", Arguments.manifestsToDownload,
					Arguments.modFolder));
			ModListManager.downloadMods();

			waitFinishProcessingMods();
			log.info("Finished downloading mods.");
		}
		log.trace("Finished Processing Mods.");
	}*/

	/*private static void waitFinishProcessingMods() throws InterruptedException {
		while (!ModListManager.getExecutorService().isTerminated()) {
			Thread.sleep(1);
		}
	}*/
}
