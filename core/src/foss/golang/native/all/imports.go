package all

import (
	_ "foss/native/app"
	_ "foss/native/common"
	_ "foss/native/config"
	_ "foss/native/delegate"
	_ "foss/native/platform"
	_ "foss/native/proxy"
	_ "foss/native/tun"
	_ "foss/native/tunnel"

	_ "golang.org/x/sync/semaphore"

	_ "foss/clash/log"
)
