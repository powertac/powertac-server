package org.powertac.server;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class TournamentSchedulerService implements InitializationService {
	static private Logger log = Logger
			.getLogger(WeatherService.class.getName());

	@Autowired
	private ServerConfiguration serverProps;
	
	
	private String tournamentSchedulerUrl = "";
	
	private int gameId = 0;
	

	public int getGameId() {
		return gameId;
	}

	public void setGameId(int gameId) {
		this.gameId = gameId;
	}

	public String getTournamentSchedulerUrl() {
		return tournamentSchedulerUrl;
	}

	public void setTournamentSchedulerUrl(String tournamentSchedulerUrl) {
		this.tournamentSchedulerUrl = tournamentSchedulerUrl;
	}

	public void ready() {
		
		String finalUrl = getTournamentSchedulerUrl() + 
		"?status=ready" + 
		"&gameId=" + getGameId();
		
		log.info("Sending game ready message to controller at: " + finalUrl);

		try {
			URL url = new URL(finalUrl);
			URLConnection conn = url.openConnection();
			// Get the response
			InputStream input = conn.getInputStream();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Jenkins failure");
		}

	}

	public String initialize(Competition competition,
			List<String> completedInits) {
		
		serverProps.configureMe(this);
		return "TournamentSchedulerService";
	}

	public void setDefaults() {
	}

}
