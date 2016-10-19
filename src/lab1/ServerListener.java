package lab1;


interface ServerListener {

    void onServerStarted();

    void onCancellationReported();

    void onCompletedComputation(int result, boolean isShortCircuited);
}
