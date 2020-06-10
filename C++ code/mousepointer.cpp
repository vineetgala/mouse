#include <Windows.h>
#include<fstream>
#include<iostream>
#include<string>
double dx,dy;
double xerror,yerror;
double vx_old,vy_old,vx,vy;
double ax_old,ay_old,ax,ay;
double time=0.02;
double scaling=5;
double drag=3.0;
std::ifstream fin;
void initialise()
{
    std::string x;
    std::string y;
    std::string z;
    double ax1,ay1,ax2,ay2,ax3,ay3;
    getline(fin,x,',');
    getline(fin,y,',');
    getline(fin,z,'\n');
    ax1=std::stod(x)-xerror;
    ay1=std::stod(y)-yerror;
    getline(fin,x,',');
    getline(fin,y,',');
    getline(fin,z,'\n');
    ax2=std::stod(x)-xerror;
    ay2=std::stod(y)-yerror;
    getline(fin,x,',');
    getline(fin,y,',');
    getline(fin,z,'\n');
    ax_old=ax3=std::stod(x)-xerror;
    ay_old=ay3=std::stod(y)-yerror;
    double vx1,vy1;
    double vx2,vy2;
    vx1=time*(ax1+ax2)/2;
    vx_old=vx2=vx1+time*(ax2+ax3)/2;
    vy1=time*(ay1+ay2)/2;
    vy_old=vy2=vy1+time*(ay2+ay3)/2;
    dx=time*(vx1+vx2)/2;
    dy=time*(vy1+vy2)/2;
    std::cout<<dx<<' '<<dy<<'\n';
}
void mousemove()
{
    vx_old/=drag;
    vy_old/=drag;
    std::string x;
    std::string y;
    std::string z;
    while(getline(fin,x,','))
    {
        getline(fin,y,',');
        getline(fin,z,'\n');
        ax=std::stod(x)-xerror;
        ay=std::stod(y)-yerror;
        vx=vx_old+time*(ax+ax_old)/2;
        vy=vy_old+time*(ay+ay_old)/2;
        dx=time*(vx+vx_old)/2;
        dy=time*(vy+vy_old)/2;
        ax_old=ax;
        ay_old=ay;
        vx_old=vx;
        vy_old=vy;
        std::cout<<dx<<' '<<dy<<'\n';
        Sleep(14);
        POINT p;
        if (GetCursorPos(&p))
        {
            SetCursorPos(p.x+(dx/scaling*1920),p.y+(dy/scaling*1080));
        }
    }
}
int calibrate()
{
    std::string x;
    std::string y;
    std::string z;
    double xtotal=0,ytotal=0;
    while(getline(fin,x,','))
    {
        getline(fin,y,',');
        getline(fin,z,'\n');
        xtotal+=std::stod(x);
        ytotal+=std::stod(y);
    }
    xerror=xtotal/1000;
    yerror=ytotal/1000;
}
int main()
{
    fin.open("calibration.csv");
    calibrate();
    fin.close();
    fin.open("calibration.csv");
    initialise();
    mousemove();
    fin.close();
    return 0;
    //SetCursorPos(100,100);
    /*INPUT iNPUT = { 0 };
    iNPUT.type=INPUT_MOUSE;
    iNPUT.mi.dwFlags=MOUSEEVENTF_LEFTDOWN;
    SendInput(1, &iNPUT, sizeof(iNPUT));
    SetCursorPos(1000,1000);
    ZeroMemory(&iNPUT, sizeof(iNPUT));
    iNPUT.type=INPUT_MOUSE;
    iNPUT.mi.dwFlags=MOUSEEVENTF_LEFTUP;
    SendInput(1, &iNPUT, sizeof(iNPUT));*/
}