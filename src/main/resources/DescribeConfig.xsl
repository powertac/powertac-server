<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
  <html>
  <body>
  <h2>Factored Customers</h2>
    <table border="1">
      <tr bgcolor="#CCCCCC">
        <th>Name</th>
        <th>PowerType (Population)</th>
        <th>Description</th>
      </tr>
      <xsl:for-each select="customers/customer">
      <tr>
        <td><xsl:value-of select="@name"/></td>
        <td>
            <table border="0">        
                <xsl:for-each select="capacityBundle">
                <tr>
                <td><nobr><xsl:value-of select="@powerType"/> (<xsl:value-of select="@population"/>)</nobr></td>
                </tr>
                </xsl:for-each>
            </table>
        </td>
        <td><xsl:value-of select="description"/></td>
      </tr>
      </xsl:for-each>
    </table>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>

