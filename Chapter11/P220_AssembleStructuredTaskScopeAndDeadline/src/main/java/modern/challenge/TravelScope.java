package modern.challenge;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.Future;
import static java.util.concurrent.Future.State.CANCELLED;
import static java.util.concurrent.Future.State.FAILED;
import static java.util.concurrent.Future.State.RUNNING;
import static java.util.concurrent.Future.State.SUCCESS;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import jdk.incubator.concurrent.StructuredTaskScope;

public class TravelScope extends StructuredTaskScope<Travel> {
    
    private static final Logger logger = Logger.getLogger(TravelScope.class.getName());
    
    private volatile RidesharingOffer ridesharingOffer;
    private volatile PublicTransportOffer publicTransportOffer;
    private volatile RidesharingException ridesharingException;
    private volatile PublicTransportException publicTransportException;    
    private volatile TimeoutException timeoutException;
    
    @Override
    protected void handleComplete(Future<Travel> future) {

        switch (future.state()) {
            case RUNNING ->
                throw new IllegalStateException("Future is still in the running state ...");
            case SUCCESS -> {
                switch(future.resultNow()) {
                    case RidesharingOffer ro -> this.ridesharingOffer = ro;
                    case PublicTransportOffer pto -> this.publicTransportOffer = pto;
                }
            }
            case FAILED -> {
                switch(future.exceptionNow()) {
                    case RidesharingException re -> this.ridesharingException = re;
                    case PublicTransportException pte -> this.publicTransportException = pte;
                    case TimeoutException te -> this.timeoutException = te;
                    case Throwable t -> throw new RuntimeException(t);
                }
            }
            case CANCELLED -> {
            }
        }
    }
    
    public TravelOffer recommendedTravelOffer() {
        
        if(timeoutException != null) {
            logger.warning("Some of the called services did not respond in time");
        }
        
        if(ridesharingOffer != null && publicTransportOffer != null) {
            return new TravelOffer(ridesharingOffer, publicTransportOffer);
        }

        if(ridesharingException != null && ridesharingOffer == null && publicTransportOffer != null) {
            
            return new TravelOffer(
                    new RidesharingOffer("none", Duration.ofMinutes(0), Duration.ofMinutes(0) ,0d), 
                    publicTransportOffer);
        }
        
        if(publicTransportException != null && publicTransportOffer == null && ridesharingOffer != null) {
            return new TravelOffer(
                    ridesharingOffer, 
                    new PublicTransportOffer("none", "none", LocalTime.MIDNIGHT));
        }                                
        
        return new TravelOffer(
                new RidesharingOffer("none", Duration.ofMinutes(0), Duration.ofMinutes(0) ,0d), 
                new PublicTransportOffer("none", "none", LocalTime.MIDNIGHT));
    }
}