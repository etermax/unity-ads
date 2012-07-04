package com.unity3d.ads.android.cache;

import java.io.File;
import java.util.ArrayList;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.UnityAdsProperties;
import com.unity3d.ads.android.UnityAdsUtils;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.IUnityAdsCampaignHandlerListener;

import android.util.Log;

public class UnityAdsCacheManager implements IUnityAdsCampaignHandlerListener {
	
	private IUnityAdsCacheListener _downloadListener = null;	
	private ArrayList<UnityAdsCampaignHandler> _downloadingHandlers = null;
	private ArrayList<UnityAdsCampaignHandler> _handlers = null;	
	private int _amountPrepared = 0;
	private int _totalCampaigns = 0;
	
	
	public UnityAdsCacheManager () {
		UnityAdsUtils.createCacheDir();
		Log.d(UnityAdsProperties.LOG_NAME, "External storagedir: " + UnityAdsUtils.getCacheDirectory());
	}
	
	public void setDownloadListener (IUnityAdsCacheListener listener) {
		_downloadListener = listener;
	}
	
	public boolean hasDownloadingHandlers () {
		return (_downloadingHandlers != null && _downloadingHandlers.size() > 0);
	}
	
	public void initCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		updateCache(activeList, pruneList);
	}
		
	public void updateCache (ArrayList<UnityAdsCampaign> activeList, ArrayList<UnityAdsCampaign> pruneList) {
		if (_downloadListener != null)
			_downloadListener.onCampaignUpdateStarted();
		
		_amountPrepared = 0;
		
		// Check cache directory and delete all files that don't match the current files in campaigns
		if (UnityAdsUtils.getCacheDirectory() != null) {
			File dir = new File(UnityAdsUtils.getCacheDirectory());
			File[] fileList = dir.listFiles();
			
			if (fileList != null) {
				for (File currentFile : fileList) {
					Log.d(UnityAdsProperties.LOG_NAME, "Checking file: " + currentFile.getName());
					if (!currentFile.getName().equals(UnityAdsProperties.PENDING_REQUESTS_FILENAME) && 
						!currentFile.getName().equals(UnityAdsProperties.CACHE_MANIFEST_FILENAME) && 
						!UnityAdsUtils.isFileRequiredByCampaigns(currentFile.getName(), activeList)) {
						UnityAdsUtils.removeFile(currentFile.getName());
					}
				}
			}
		}
		
		// Prune -list contains campaigns that were still in cache but not in the received videoPlan.
		// Therefore they will not be put into cache. Check that the existing videos for those
		// campaigns are not needed by current active ones and remove them if needed.
		if (pruneList != null) {
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Pruning old campaigns");
			for (UnityAdsCampaign campaign : pruneList) {
				UnityAds.cachemanifest.removeCampaignFromManifest(campaign.getCampaignId());
				if (!UnityAdsUtils.isFileRequiredByCampaigns(campaign.getVideoUrl(), activeList)) {
					UnityAdsUtils.removeFile(campaign.getVideoUrl());
				}
			}
		}
		
		// Active -list contains campaigns that came with the videoPlan
		if (activeList != null) {
			_totalCampaigns = activeList.size();
			Log.d(UnityAdsProperties.LOG_NAME, "Updating cache: Going through active campaigns");			
			for (UnityAdsCampaign campaign : activeList) {
				UnityAdsCampaignHandler campaignHandler = new UnityAdsCampaignHandler(campaign, activeList);
				addToUpdatingHandlers(campaignHandler);
				campaignHandler.setListener(this);
				campaignHandler.initCampaign();
				
				if (campaignHandler.hasDownloads()) {
					addToDownloadingHandlers(campaignHandler);
				}					
			}
		}
	}

	
	// EVENT METHDOS
	
	@Override
	public void onCampaignHandled(UnityAdsCampaignHandler campaignHandler) {
		_amountPrepared++;
		removeFromDownloadingHandlers(campaignHandler);
		removeFromUpdatingHandlers(campaignHandler);
		_downloadListener.onCampaignReady(campaignHandler);
		
		if (_amountPrepared == _totalCampaigns)
			_downloadListener.onAllCampaignsReady();
	}	
	
	
	// INTERNAL METHODS
	
	private void removeFromUpdatingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_handlers != null)
			_handlers.remove(campaignHandler);
	}
	
	private void addToUpdatingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_handlers == null)
			_handlers = new ArrayList<UnityAdsCampaignHandler>();
		
		_handlers.add(campaignHandler);
	}
	
	private void removeFromDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers != null)
			_downloadingHandlers.remove(campaignHandler);
	}
	
	private void addToDownloadingHandlers (UnityAdsCampaignHandler campaignHandler) {
		if (_downloadingHandlers == null)
			_downloadingHandlers = new ArrayList<UnityAdsCampaignHandler>();
		
		_downloadingHandlers.add(campaignHandler);
	}
}
