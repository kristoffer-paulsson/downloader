package org.example.downloader;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class DebianWorkerIterator implements Iterator<DebianWorker> {
    private final Iterator<DebianPackage> packages;
    private final ConfigManager configManager;
    private final DebianMirrorCache mirrorCache;


    DebianWorkerIterator(InversionOfControl ioc, List<DebianPackage> packages) {
        this.configManager = ioc.resolve(ConfigManager.class);
        this.mirrorCache = ioc.resolve(DebianMirrorCache.class);
        this.packages = packages.iterator();
    }

    @Override
    public boolean hasNext() {
        return packages.hasNext();
    }

    @Override
    public DebianWorker next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more items in the package");
        }
        return new DebianWorker(packages.next(), configManager, mirrorCache.getNextMirror());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported");
    }
}