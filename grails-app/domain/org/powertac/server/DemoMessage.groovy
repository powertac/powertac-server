package org.powertac.server

class DemoMessage {

  String contents

  static constraints = {
    contents(nullable: false)
  }

  static mapping = {
    contents type: "text"
  }
}
