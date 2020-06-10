-- --------------------------------------------------------
-- 主机:                           127.0.0.1
-- 服务器版本:                        10.4.12-MariaDB - mariadb.org binary distribution
-- 服务器OS:                        Win64
-- HeidiSQL 版本:                  10.2.0.5599
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping structure for table test.fflogs
CREATE TABLE IF NOT EXISTS `fflogs` (
  `tmid` bigint(15) unsigned NOT NULL,
  `id` int(4) unsigned NOT NULL,
  `type` int(4) unsigned NOT NULL,
  `typestr` varchar(15) NOT NULL DEFAULT '-1',
  `content` varchar(100) DEFAULT NULL,
  `gettime` datetime DEFAULT NULL,
  `bs` blob DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

-- Data exporting was unselected.

-- Dumping structure for table test.ob_taskb
CREATE TABLE IF NOT EXISTS `ob_taskb` (
  `id` bigint(20) NOT NULL,
  `sbid` int(6) NOT NULL,
  `effective` tinyint(1) NOT NULL DEFAULT 0,
  `speedtime` datetime DEFAULT NULL,
  `longitude` varchar(30) DEFAULT NULL,
  `latitude` varchar(30) DEFAULT NULL,
  `speed` decimal(16,4) DEFAULT NULL,
  `height` decimal(16,4) DEFAULT NULL,
  `direction` varchar(4) DEFAULT NULL,
  `flow` decimal(16,6) DEFAULT NULL,
  `sumflow` decimal(16,6) DEFAULT NULL,
  `temperature` decimal(16,6) DEFAULT NULL,
  `pressure` decimal(16,6) DEFAULT NULL,
  `sum_flow` float NOT NULL,
  PRIMARY KEY (`id`,`sbid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
