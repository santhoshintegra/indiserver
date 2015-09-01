# Introduction #

This page describes the relevant INDI-Properities and it's Elements used for telescope control.


# Details #

The following INDI-Properties and Elements should be implemented by any telescope-driver and client with the **exact names and types** (to be compatible with original INDIlib drivers):

| **Property Name** | **Type** | **Element Name** | **Description** |
|:------------------|:---------|:-----------------|:----------------|
| EQUATORIAL\_EOD\_COORD\_REQUEST | Number Property |                  | _Write-Only_ Property for new Target-Coordinates. If new coords are set, telescope will slew immediately |
|                   | Double   | RA               | Right ascension of target |
|                   | Double   | DEC              | Declination of target |
| EQUATORIAL\_EOD\_COORD | Number Property |                  | _Read-Only_ Property for Coords the telescope is currently pointing at |
|                   | Double   | RA               | Right ascension the telescope is pointing at |
|                   | Double   | DEC              | Declination the telescope is pointing at |
| ON\_COORD\_SET    | Switch Property |                  | Slew or sync on new coords |
|                   | ONE\_OF\_MANY | SLEW             | Slew the telescope, if new target coords are set |
|                   | ONE\_OF\_MANY | SYNC             | Sync current coords to set coords |
| TELESCOPE\_ABORT\_MOTION | Switch Property |                  | Abort all current motion of telescope |
|                   | ONE\_OF\_MANY | ABORT            | ON = Abort motion |

The following Properties are optional, but highly recommended. Only with these data properly set, the telescope will slew to the right coords.
**Set these values _before_ setting new target coords!**

| **Property Name** | **Type** | **Element Name** | **Description** |
|:------------------|:---------|:-----------------|:----------------|
| TIME\_UTC         | Text Property |                  | Current Time and Date as INDI-Timestamp |
|                   | Timestamp | UTC              | Current Time and Date in UTC |
| TIME\_UTC\_OFFSET | Number Property |                  | UTC offset to yield local time |
|                   | Double   | OFFSET           | Offset as double |
| GEOGRAPHIC\_COORD | Number Property |                  | Geographical coords of the current observation site |
|                   | Double   | LAT              | Latitude of current observation site |
|                   | Double   | LONG             | Longitude of current observation site |