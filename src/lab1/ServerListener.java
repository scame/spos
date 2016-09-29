package lab1;


interface ServerListener {

    void onServerStarted();

    void onFailureReported(String cause);

    void onCompletedComputation(int result, boolean isShortCircuited);
}
