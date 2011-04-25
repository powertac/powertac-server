

class BootStrap {

  def competitionControlService

  def init = { servletContext ->
    competitionControlService.preGame()
  }
  
  def destroy = {
  }
}
