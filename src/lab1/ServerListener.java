package lab1;


interface ServerListener {

    void onServerStarted();

    void onFailReported(String cause);

    void onCompletedComputation(int result, boolean isShortCircuited);
}
