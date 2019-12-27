package simplespider.simplespider.dao.mem;

import simplespider.simplespider.dao.LinkDao;
import simplespider.simplespider.throttle.host.HostThrottler;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Mike on 18.02.2017.
 */

class MemLinkDao implements LinkDao {
    private final MemDbHelper memDbHelper;


    public MemLinkDao(MemDbHelper memDbHelper) {
        this.memDbHelper = memDbHelper;
    }

	@Override
    public String removeNextAndCommit() {
        SimpleSet<String> queue = memDbHelper.getQueue();
        HostThrottler hostThrottler = memDbHelper.getHostThrottler();

        // Determine how many Link objects has to be loaded into memory for host throttling
        final int maxUrlsAtOnce = hostThrottler.getUrlsAtOnce();

        // Required for host throttler
        final List<String> urls = new ArrayList<String>(maxUrlsAtOnce);

        // Activate all queued links, that are be checked for the best one
        for (int i = 0; i < maxUrlsAtOnce; i++) {
            String url = queue.remove();
            if (url == null) {
                // no more available
                break;
            }
            if (url.length() != 0 ) {
                urls.add(url);
            }
        }

        // If list is empty, there is none URL that could be loaded
        if (urls.isEmpty()) {
            return null;
        }

        // Get the best URL
        final String nextUrlString = hostThrottler.getBestFittingString(urls);
        if (nextUrlString != null) {
            urls.remove(nextUrlString);
        }

        // Put remaining urls back to queue
        queue.addAll(urls);


        return nextUrlString;
    }

	@Override
    public void saveAndCommit(String link) {
        saveForced(link);
    }

	@Override
    public void saveForced(String link) {
        SimpleSet<String> queue = memDbHelper.getQueue();
        queue.put(link);
    }
}
