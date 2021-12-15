package org.magnum.dataup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class VideoUploadController {
	private static final String SERVER = "http://localhost:8080";
	static HttpServletRequest request;
	private static List<Video> videoList = new ArrayList<>();
	private Map<Long, MultipartFile> dataList = new HashMap<>();
	private VideoFileManager videoDataMgr;
	private static final AtomicLong currentId = new AtomicLong(0L);
	private static Map<Long,Video> videos = new HashMap<Long, Video>();

	//Method to save video meta data
	public static Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

	//Method to set id of a video
	private static void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}

	//Method to get data url for a video
	private static String getDataUrl(long videoId){
		String url = getUrlBaseForLocalServer()+ "/video/" + videoId + "/data";
		return url;
	}

	private static String getUrlBaseForLocalServer() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
		String base = 
				"http://"+request.getServerName() 
				+ ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		return base;
	}

	// To add videos manually 
	static {
		Video video = Video.create().withContentType("video/mpeg").withDuration(123).
				withSubject("Cloud Services").withTitle("Programming cloud services for...").build();
		save(video);
		video.setDataUrl(getDataUrl(video.getId()));
		Video video1 = Video.create().withContentType("video/mpeg").withDuration(456).
				withSubject("Springboot").withTitle("Springboot microservices...").build();
		save(video1);
		video1.setDataUrl(getDataUrl(video1.getId()));
		Video video2 = Video.create().withContentType("video/mpeg").withDuration(789).
				withSubject("Hibernate").withTitle("Hibernate JPA...").build();
		save(video2);
		video2.setDataUrl(getDataUrl(video2.getId()));
		videoList.add(video);
		videoList.add(video1);
		videoList.add(video2);
	}

	// Method to get list of videos
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody
	List<Video> addGetVideo(HttpServletResponse response) throws IOException {
		if(response.getStatus() != 200) {
			response.sendError(response.getStatus());
		}
		return videoList;
	}
	
	//Method to add a video
	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideoMetadata(@RequestBody Video v, HttpServletResponse response) throws IOException {
		if(response.getStatus() != 200) {
			response.sendError(response.getStatus());
		}
		save(v);
		v.setDataUrl(getDataUrl(v.getId()));
		videoList.add(v);
		return v;
	}

	//Method to add a video as Multipart file
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST, consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
	public @ResponseBody
	VideoStatus setVideoData(@RequestParam("data") MultipartFile videoData, @PathVariable("id") long id, HttpServletResponse response) throws IOException {
		Video new_video = videos.get(id);
		if (!videoList.contains(new_video) || new_video.getId() != id) {
			response.sendError(404);
		} else {
			videoDataMgr = VideoFileManager.get();
			videoDataMgr.saveVideoData(new_video, videoData.getInputStream());
			dataList.put(id, videoData);
		}
		return new VideoStatus(VideoStatus.VideoState.READY);
	}

	// Method to retrieve a video saved as Multipart file
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
	public @ResponseBody
	MultipartFile serveSomeVideo(Video v,HttpServletResponse response, @PathVariable("id") long id) throws IOException {   
		if (!dataList.containsKey(id)) {
			response.sendError(404);
		} else {
			videoDataMgr = VideoFileManager.get();
			videoDataMgr.copyVideoData(v, response.getOutputStream());
		}
		return dataList.get((int)id-1);
	}
}
