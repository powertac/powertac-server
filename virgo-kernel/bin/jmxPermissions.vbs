configFolder = Wscript.Arguments.Item(0)

'WScript.Echo "Fixing permissions on " & configFolder

Set objWMIService = GetObject("winmgmts:\\.\root\CIMV2") 'Load up WMI with the right dll

Dim files(0)
files(0) = "org.eclipse.virgo.kernel.jmxremote.access.properties"

For Each file In files
	updateInheritance(configFolder & file)
	updateOwnership(configFolder & file)
	updatePermissions(configFolder & file)
Next 

Sub updateInheritance(file)
	'WScript.Echo "Updating inheritance of " & file
	
	Const SE_DACL_PRESENT = 4
	Const SE_DACL_PROTECTED = 4096
	Const SE_SELF_RELATIVE = 32768

	Set objFileSecSetting = objWMIService.Get("Win32_LogicalFileSecuritySetting.Path='" & file & "'")
    objFileSecSetting.GetSecurityDescriptor objSecurityDescriptor

	objSecurityDescriptor.ControlFlags = SE_DACL_PRESENT + SE_DACL_PROTECTED + SE_SELF_RELATIVE

	Set objMethod = objFileSecSetting.Methods_("SetSecurityDescriptor")
	Set objInParam = objMethod.inParameters.SpawnInstance_()
	objInParam.Properties_.item("Descriptor") = objSecurityDescriptor
	objFileSecSetting.ExecMethod_ "SetSecurityDescriptor", objInParam
	
	'WScript.Echo "Updated inheritance of " & file
End Sub

Sub updateOwnership(file)
	'WScript.Echo "Updating ownership of " & file
	Set objDataFile = objWMIService.Get("CIM_DataFile.Name='" & file & "'")

	Set objMethod = objDataFile.Methods_("TakeOwnerShipEx")
	Set objInParam = objMethod.inParameters.SpawnInstance_()

	objDataFile.ExecMethod_ "TakeOwnerShipEx", objInParam

	'WScript.Echo "Updated ownership of " & file
End Sub

Sub updatePermissions(file)	
	'WScript.Echo "Updating permissions of " & file
	
	Set objFileSecSetting = objWMIService.Get("Win32_LogicalFileSecuritySetting.Path='" & file & "'")
    objFileSecSetting.GetSecurityDescriptor objSecurityDescriptor

	Set WshNetwork = WScript.CreateObject("WScript.Network")
	
	Dim foundAce
	foundAce = "false"
	
	'Search for an ACE for the current user as there is no robust, portable way of creating such an ACE from scratch in VBScript.
	Dim specificAce(0)
	For Each ace in objSecurityDescriptor.DACL
		If ace.Trustee.Name = WshNetwork.UserName Then
			Set specificAce(0) = ace
			foundAce = "true"
		End If
	Next
	
	If foundAce = "true" Then
		objSecurityDescriptor.DACL = specificAce

		Set objMethod = objFileSecSetting.Methods_("SetSecurityDescriptor")
		Set objInParam = objMethod.inParameters.SpawnInstance_()
		objInParam.Properties_.item("Descriptor") = objSecurityDescriptor
		objFileSecSetting.ExecMethod_ "SetSecurityDescriptor", objInParam
		
		'WScript.Echo "Updated permissions of " & file
	Else
		WScript.Echo "WARNING: jmxPermissions.vbs did not update the permissions of " & file & ". Check the file has the correct permissions."
	End If
	
End Sub
