# Power TAC server generic component

This package is intended to be forked to create new components for the Power TAC server. It contains the basic structure and dependencies for
most components, including domain types, services, and unit/integration tests.

After forking, you will need to follow approximately the following process to start a new component:

* Change the package names from org.powertac.generic to whatever package name makes sense.
* Change class names to make them clearly descriptive of what they do.
* Fill in header comments and @author tags - this is needed to match up with the copyright notice.
* Implement data representation and behaviors as needed.
* Write tests.