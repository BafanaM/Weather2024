package repository

import model.LocationData
import utils.RequestCompleteListener

interface LocationProviderInterface {
    fun getCurrentUserLocation(callback: RequestCompleteListener<LocationData>)
}