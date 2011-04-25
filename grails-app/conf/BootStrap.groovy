

class BootStrap {

  def competitionControlService

  def init = { servletContext ->
    log.info("Server BootStrap")
    competitionControlService.preGame()
  }
  
  def destroy = {
  }
}
