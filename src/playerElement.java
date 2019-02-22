import java.awt.Color;

class playerElement {
	String displayName;
	int uid;
	
	double x;
	double y;
	double radius;
	Color color;
	Color tColor;
	int status=1;
	int explosion=0;
	
	int team;
	
	boolean fired;
	int shield=0;
	int angle;
	int power;
	
	double mX;
	double mY;
	double mRadius;
	double mxVelocity;
	double myVelocity;
	boolean mWorm;
	int mStatus;
	int mExplosion;
	double[] mxDebP;
	double[] myDebP;
	double[] mxDebV;
	double[] myDebV;
	Color mColor;
	int mX1;
	int mY1;
	int mX2;
	int mY2;
	int mX3;
	int mY3;
	int mX4;
	int mY4;
	
	boolean hyper;
	int hStep=0;
	double hX;
	double hY;
}