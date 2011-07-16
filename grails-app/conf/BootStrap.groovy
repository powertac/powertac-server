

class BootStrap {

  def competitionControlService
  def participantManagementService

  def init = { servletContext ->
    log.info("Server BootStrap")
    participantManagementService.initialize()
    competitionControlService.preGame()
  }
  
  def destroy = {
  }
}
