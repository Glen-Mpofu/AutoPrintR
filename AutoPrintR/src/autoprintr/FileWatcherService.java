package autoprintr;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class FileWatcherService {
    private String watchFolder;
    private final FileHandler handler;

    public interface FileHandler {
        void onFileCreated(File file);
    }

    public FileWatcherService(String folder, FileHandler handler) {
        this.watchFolder = folder;
        this.handler = handler;
    }

    public void setWatchFolder(String folder) {
        this.watchFolder = folder;
    }

    public void startWatching() {
        new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(watchFolder);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        File newFile = new File(watchFolder, event.context().toString());
                        handler.onFileCreated(newFile);
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
